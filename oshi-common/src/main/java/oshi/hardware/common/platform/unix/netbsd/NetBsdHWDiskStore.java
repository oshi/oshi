/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.bsd.disk.Disklabel;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Quartet;

/**
 * NetBSD hard disk implementation.
 */
@ThreadSafe
public final class NetBsdHWDiskStore extends AbstractHWDiskStore {

    // dk1 at ld4: "bce3cb3f-b5ff-4aac-8049-5f748b710db0", 133807967 blocks at 409728, type: ffs
    private static final Pattern DMESG_WEDGE = Pattern
            .compile("\\b(dk\\d+) at (\\w+): \"(.*)\", (\\d+) blocks at \\d+, type: (\\S*)");
    // ld4: 65536 MB, 16383 cyl, 16 head, 63 sec, 512 bytes/sect x 134217728 sectors
    private static final Pattern DMESG_GEOMETRY = Pattern.compile("\\b(\\w+): .* (\\d+) bytes/sect x \\d+ sectors");

    private final Supplier<List<String>> iostat;

    // Package-private for the fixture test, which supplies its own iostat output
    NetBsdHWDiskStore(String name, String model, String serial, long size, Supplier<List<String>> iostatSupplier) {
        super(name, model, serial, size);
        this.iostat = iostatSupplier;
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        List<HWDiskStore> diskList = new ArrayList<>();
        List<String> dmesg = queryDmesg();
        Map<String, List<HWPartition>> wedges = parseWedges(dmesg, queryMounts(), Disklabel::getMajorMinor);
        Set<String> wedgeNames = new HashSet<>();
        for (List<HWPartition> parts : wedges.values()) {
            for (HWPartition part : parts) {
                wedgeNames.add(part.getIdentification());
            }
        }
        Supplier<List<String>> iostatSupplier = memoize(NetBsdHWDiskStore::queryIostat, defaultExpiration());

        // Get list of disks from sysctl
        // NetBSD: hw.disknames = ld0 fd0 dk0 dk1 cd0 (space-separated, no colon suffix)
        String[] devices = ParseUtil.whitespaces.split(BsdSysctlUtil.sysctl("hw.disknames", "").trim(), -1);
        NetBsdHWDiskStore store;
        for (String diskName : devices) {
            // A wedge is a partition of the disk it sits on, and is listed there instead
            if (wedgeNames.contains(diskName)) {
                continue;
            }
            // Try disklabel first (needs root)
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            String model = diskdata.getA();
            long size = diskdata.getC();
            if (size <= 1) {
                // NetBSD dmesg format for physical disks:
                // ld0: 200 GB, 16383 cyl, 16 head, 63 sec, 512 bytes/sect x 419430400 sectors
                Pattern diskGeom = Pattern.compile(diskName + ": .* (\\d+) bytes/sect x (\\d+) sectors");
                // NetBSD dmesg format for wedge devices (dk*), for one whose parent was not found:
                // dk0 at ld0: "uuid", 406845440 blocks at 2048, type: ffs
                Pattern wedgeBlocks = Pattern.compile(diskName + " at (\\w+): .*, (\\d+) blocks at .*");
                // Model from "device at bus targ N lun N: <Vendor, Model, Rev>"
                // but NOT from "features: 0x...<FLAGS>" lines
                Pattern diskAt = Pattern.compile(".*" + diskName + " at .* lun \\d+: <(.+)>.*");
                for (String line : dmesg) {
                    Matcher m = diskGeom.matcher(line);
                    if (m.find()) {
                        long bytesPerSector = ParseUtil.parseLongOrDefault(m.group(1), 512L);
                        long sectors = ParseUtil.parseLongOrDefault(m.group(2), 0L);
                        size = bytesPerSector * sectors;
                        break;
                    }
                    m = wedgeBlocks.matcher(line);
                    if (m.find()) {
                        long blocks = ParseUtil.parseLongOrDefault(m.group(2), 0L);
                        size = blocks * 512L;
                        break;
                    }
                    m = diskAt.matcher(line);
                    if (m.matches()) {
                        model = m.group(1);
                    }
                }
            }
            store = new NetBsdHWDiskStore(diskName, model, diskdata.getB(), size, iostatSupplier);
            // On a disk carrying wedges, the wedges are its partitions; any disklabel describes the same space
            List<HWPartition> diskWedges = wedges.get(diskName);
            store.setPartitionList(diskWedges == null ? diskdata.getD() : diskWedges);
            store.updateAttributes();

            diskList.add(store);
        }
        return diskList;
    }

    /**
     * Parses the wedges ({@code dk} devices) announced in the kernel message buffer, grouped by the disk each one sits
     * on. Package-private for testing.
     * <p>
     * A wedge's size is counted in sectors of its parent disk, so the parent's sector size comes from its geometry
     * line, defaulting to 512 bytes. The quoted wedge name is the partition label if one was set, and otherwise its
     * GUID. A wedge appearing more than once, as it does when the live buffer and the boot copy are both read, is
     * listed once, from its first appearance.
     *
     * @param dmesg            the kernel messages
     * @param mounts           mount points keyed by device name, such as {@code dk1}
     * @param majorMinorLookup resolves a device name and suffix to its major and minor numbers
     * @return the wedges of each disk, keyed by the disk's name
     */
    static Map<String, List<HWPartition>> parseWedges(List<String> dmesg, Map<String, String> mounts,
            BiFunction<String, String, Pair<Integer, Integer>> majorMinorLookup) {
        Map<String, Long> sectorSizes = new HashMap<>();
        for (String line : dmesg) {
            Matcher m = DMESG_GEOMETRY.matcher(line);
            if (m.find()) {
                sectorSizes.putIfAbsent(m.group(1), ParseUtil.parseLongOrDefault(m.group(2), 512L));
            }
        }
        Map<String, List<HWPartition>> wedges = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (String line : dmesg) {
            Matcher m = DMESG_WEDGE.matcher(line);
            if (m.find() && seen.add(m.group(1))) {
                String name = m.group(1);
                String parent = m.group(2);
                String wedgeName = m.group(3);
                String uuid = ParseUtil.parseUuidOrDefault(wedgeName, "");
                long size = ParseUtil.parseLongOrDefault(m.group(4), 0L) * sectorSizes.getOrDefault(parent, 512L);
                Pair<Integer, Integer> majorMinor = majorMinorLookup.apply(name, "");
                wedges.computeIfAbsent(parent, p -> new ArrayList<>())
                        .add(new HWPartition(name, name, m.group(5), uuid, uuid.isEmpty() ? wedgeName : "", size,
                                majorMinor.getA(), majorMinor.getB(), mounts.getOrDefault(name, "")));
            }
        }
        return wedges;
    }

    /**
     * Parses {@code df} output into mount points keyed by device name. Package-private for testing.
     *
     * @param df the output of {@code df}
     * @return mount points keyed by the device name without its {@code /dev/} prefix
     */
    static Map<String, String> parseMounts(List<String> df) {
        Map<String, String> mounts = new HashMap<>();
        for (String line : df) {
            if (line.startsWith("/dev/")) {
                // Six columns; the last, the mount point, may contain spaces
                String[] split = ParseUtil.whitespaces.split(line, 6);
                if (split.length == 6) {
                    mounts.put(split[0].substring(5), split[5]);
                }
            }
        }
        return mounts;
    }

    /**
     * Reads the kernel messages, followed by the copy saved at boot. The live buffer holds wedges added since boot, and
     * the boot copy still holds the disks once the live buffer has wrapped past them.
     */
    private static List<String> queryDmesg() {
        List<String> dmesg = new ArrayList<>(ExecutingCommand.runNative("dmesg"));
        dmesg.addAll(FileUtil.readFile("/var/run/dmesg.boot", false));
        return dmesg;
    }

    private static Map<String, String> queryMounts() {
        return parseMounts(ExecutingCommand.runNative("df"));
    }

    @Override
    public synchronized boolean updateAttributes() {
        long now = System.currentTimeMillis();
        boolean diskFound = false;
        for (String line : iostat.get()) {
            String[] split = ParseUtil.whitespaces.split(line.trim(), -1);
            // iostat -x -I output (cumulative totals, 9 fields):
            // device read KB/t xfr time MB write KB/t xfr time MB
            // ld0 26.36 2087 8.23 53.72 25.42 74725 8.23 1855.30
            // [0] [1] [2] [3] [4] [5] [6] [7] [8]
            if (split.length >= 9 && split[0].equals(getName())) {
                diskFound = true;
                this.reads = ParseUtil.parseLongOrDefault(split[2], 0L);
                this.writes = ParseUtil.parseLongOrDefault(split[6], 0L);
                // Bytes come from the MB totals, not from KB-per-transfer times transfers: iostat rounds KB/t to two
                // decimals, and multiplying that by a transfer count scales the rounding error up with it, enough that
                // two readings seconds apart can go backwards. Rounding a total cannot.
                this.readBytes = (long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024 * 1024);
                this.writeBytes = (long) (ParseUtil.parseDoubleOrDefault(split[8], 0d) * 1024 * 1024);
                // Both time columns carry the same drive-busy total (cur.time[dn] in iostat's drive_statsx), which
                // covers reads and writes together, so it is counted once.
                this.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1000);
                this.timeStamp = now;
                break;
            }
        }
        return diskFound;
    }

    private static List<String> queryIostat() {
        return ExecutingCommand.runNative("iostat -x -I");
    }
}
