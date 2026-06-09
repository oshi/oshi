/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

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
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;
import oshi.util.tuples.Quartet;

/**
 * OpenBSD hard disk implementation.
 */
@ThreadSafe
public final class OpenBsdHWDiskStoreJNA extends AbstractHWDiskStore {

    private final Supplier<List<String>> iostat = memoize(OpenBsdHWDiskStoreJNA::querySystatIostat,
            defaultExpiration());

    private OpenBsdHWDiskStoreJNA(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        List<HWDiskStore> diskList = new ArrayList<>();
        List<String> dmesg = null; // Lazily fetch in loop if needed

        // Get list of disks from sysctl
        // hw.disknames=sd0:2cf69345d371cd82,cd0:,sd1:
        String[] devices = OpenBsdSysctlUtil.sysctl("hw.disknames", "").split(",");
        OpenBsdHWDiskStoreJNA store;
        String diskName;
        for (String device : devices) {
            diskName = device.split(":")[0];
            // get partitions using disklabel command (requires root)
            Quartet<String, String, Long, List<HWPartition>> diskdata = Disklabel.getDiskParams(diskName);
            String model = diskdata.getA();
            long size = diskdata.getC();
            if (size <= 1) {
                if (dmesg == null) {
                    dmesg = ExecutingCommand.runNative("dmesg");
                }
                Pattern diskAt = Pattern.compile(diskName + " at .*<(.+)>.*");
                Pattern diskMB = Pattern
                        .compile(diskName + ":.* (\\d+)MB, (?:(\\d+) bytes\\/sector, )?(?:(\\d+) sectors).*");
                for (String line : dmesg) {
                    Matcher m = diskAt.matcher(line);
                    if (m.matches()) {
                        model = m.group(1);
                    }
                    m = diskMB.matcher(line);
                    if (m.matches()) {
                        // Group 3 is sectors
                        long sectors = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                        // Group 2 is optional capture of bytes per sector
                        long bytesPerSector = ParseUtil.parseLongOrDefault(m.group(2), 0L);
                        if (bytesPerSector == 0 && sectors > 0) {
                            // if we don't have bytes per sector guess at it based on total size and number
                            // of sectors
                            // Group 1 is size in MB, which may round
                            size = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 20;
                            // Estimate bytes per sector. Should be "near" a power of 2
                            bytesPerSector = size / sectors;
                            // Multiply by 1.5 and round down to nearest power of 2:
                            bytesPerSector = Long.highestOneBit(bytesPerSector + (bytesPerSector >> 1));
                        }
                        size = bytesPerSector * sectors;
                        break;
                    }
                }
            }
            store = new OpenBsdHWDiskStoreJNA(diskName, model, diskdata.getB(), size);
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
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length >= 6 && split[0].equals(getName())) {
                diskFound = true;
                setReadBytes(ParseUtil.parseMultipliedToLongs(split[1]));
                setWriteBytes(ParseUtil.parseMultipliedToLongs(split[2]));
                setReads((long) ParseUtil.parseDoubleOrDefault(split[3], 0d));
                setWrites((long) ParseUtil.parseDoubleOrDefault(split[4], 0d));
                // In seconds, multiply for ms
                setTransferTime((long) (ParseUtil.parseDoubleOrDefault(split[5], 0d) * 1000));
                setTimeStamp(now);
            }
        }
        return diskFound;
    }

    private static List<String> querySystatIostat() {
        return ExecutingCommand.runNative("systat -ab iostat");
    }
}
