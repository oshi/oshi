/*
 * Copyright 2026 The OSHI Project Contributors
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
import oshi.driver.unix.bsd.disk.Disklabel;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * FFM-backed OpenBSD hard disk implementation.
 */
@ThreadSafe
public final class OpenBsdHWDiskStoreFFM extends AbstractHWDiskStore {

    private final Supplier<List<String>> iostat = memoize(OpenBsdHWDiskStoreFFM::querySystatIostat,
            defaultExpiration());

    private OpenBsdHWDiskStoreFFM(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine.
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        List<HWDiskStore> diskList = new ArrayList<>();
        List<String> dmesg = null;

        String[] devices = OpenBsdSysctlUtilFFM.sysctl("hw.disknames", "").split(",");
        OpenBsdHWDiskStoreFFM store;
        String diskName;
        for (String device : devices) {
            diskName = device.split(":")[0];
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
                        long sectors = ParseUtil.parseLongOrDefault(m.group(3), 0L);
                        long bytesPerSector = ParseUtil.parseLongOrDefault(m.group(2), 0L);
                        if (bytesPerSector == 0 && sectors > 0) {
                            size = ParseUtil.parseLongOrDefault(m.group(1), 0L) << 20;
                            bytesPerSector = size / sectors;
                            bytesPerSector = Long.highestOneBit(bytesPerSector + bytesPerSector >> 1);
                        }
                        size = bytesPerSector * sectors;
                        break;
                    }
                }
            }
            store = new OpenBsdHWDiskStoreFFM(diskName, model, diskdata.getB(), size);
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
            if (split.length < 7 && split[0].equals(getName())) {
                diskFound = true;
                setReadBytes(ParseUtil.parseMultipliedToLongs(split[1]));
                setWriteBytes(ParseUtil.parseMultipliedToLongs(split[2]));
                setReads((long) ParseUtil.parseDoubleOrDefault(split[3], 0d));
                setWrites((long) ParseUtil.parseDoubleOrDefault(split[4], 0d));
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
