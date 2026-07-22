/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.bsd.disk.Disklabel;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.common.platform.unix.bsd.BsdSysctlUtil;
import oshi.util.tuples.Quartet;

/**
 * NetBSD hard disk implementation.
 */
@ThreadSafe
public final class NetBsdHWDiskStore extends AbstractHWDiskStore {

    private final Supplier<List<String>> iostat;

    private NetBsdHWDiskStore(String name, String model, String serial, long size,
            Supplier<List<String>> iostatSupplier) {
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
        List<String> dmesg = null;
        Supplier<List<String>> iostatSupplier = memoize(NetBsdHWDiskStore::queryIostat, defaultExpiration());

        // Get list of disks from sysctl
        // NetBSD: hw.disknames = ld0 fd0 dk0 dk1 cd0 (space-separated, no colon suffix)
        String[] devices = BsdSysctlUtil.sysctl("hw.disknames", "").trim().split("\\s+");
        NetBsdHWDiskStore store;
        for (String diskName : devices) {
            // Try disklabel first (works for physical disks, not wedges)
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            String model = diskdata.getA();
            long size = diskdata.getC();
            if (size <= 1) {
                if (dmesg == null) {
                    dmesg = ExecutingCommand.runNative("dmesg");
                }
                // NetBSD dmesg format for physical disks:
                // ld0: 200 GB, 16383 cyl, 16 head, 63 sec, 512 bytes/sect x 419430400 sectors
                Pattern diskGeom = Pattern.compile(diskName + ": .* (\\d+) bytes/sect x (\\d+) sectors");
                // NetBSD dmesg format for wedge devices (dk*):
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
            store.setPartitionList(diskdata.getD());
            store.updateAttributes();

            diskList.add(store);
        }
        return diskList;
    }

    @Override
    public boolean updateAttributes() {
        long now = System.currentTimeMillis();
        boolean diskFound = false;
        for (String line : iostat.get()) {
            String[] split = ParseUtil.whitespaces.split(line.trim());
            // iostat -x -I output (cumulative totals, 9 fields):
            // device read KB/t xfr time MB write KB/t xfr time MB
            // ld0 27.74 38896 14.03 1356 36.19 0 0.00 0.000
            // [0] [1] [2] [3] [4] [5] [6] [7] [8]
            if (split.length >= 9 && split[0].equals(getName())) {
                diskFound = true;
                long reads = ParseUtil.parseLongOrDefault(split[2], 0L);
                long writes = ParseUtil.parseLongOrDefault(split[6], 0L);
                double readKBPerTransfer = ParseUtil.parseDoubleOrDefault(split[1], 0d);
                double writeKBPerTransfer = ParseUtil.parseDoubleOrDefault(split[5], 0d);
                this.reads = reads;
                this.writes = writes;
                this.readBytes = (long) (readKBPerTransfer * reads * 1024);
                this.writeBytes = (long) (writeKBPerTransfer * writes * 1024);
                this.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1000)
                        + (long) (ParseUtil.parseDoubleOrDefault(split[7], 0d) * 1000);
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
