/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
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
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Shared OpenBSD HWDiskStore logic. Subclasses provide the sysctl backend (JNA or FFM).
 */
@ThreadSafe
public abstract class OpenBsdHWDiskStore extends AbstractHWDiskStore {

    private final Supplier<List<String>> iostat = memoize(OpenBsdHWDiskStore::querySystatIostat, defaultExpiration());

    protected OpenBsdHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine using the provided sysctl accessor.
     *
     * @param <T>       the concrete disk store type
     * @param sysctlStr reads a string sysctl given (name, default)
     * @param factory   constructs the platform-specific instance
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    protected static <T extends OpenBsdHWDiskStore> List<HWDiskStore> getDisks(
            BiFunction<String, String, String> sysctlStr, DiskStoreFactory<T> factory) {
        List<HWDiskStore> diskList = new ArrayList<>();
        List<String> dmesg = null;

        String disknames = sysctlStr.apply("hw.disknames", "");
        if (disknames.isEmpty()) {
            return diskList;
        }
        String[] devices = disknames.split(",");
        T store;
        String diskName;
        for (String device : devices) {
            diskName = device.split(":")[0];
            if (diskName.isEmpty()) {
                continue;
            }
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
                            bytesPerSector = Long.highestOneBit(bytesPerSector + (bytesPerSector >> 1));
                        }
                        size = bytesPerSector * sectors;
                        break;
                    }
                }
            }
            store = factory.create(diskName, model, diskdata.getB(), size);
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
                setTransferTime((long) (ParseUtil.parseDoubleOrDefault(split[5], 0d) * 1000));
                setTimeStamp(now);
            }
        }
        return diskFound;
    }

    private static List<String> querySystatIostat() {
        return ExecutingCommand.runNative("systat -ab iostat");
    }

    /**
     * Factory for creating platform-specific OpenBsdHWDiskStore instances.
     *
     * @param <T> the concrete disk store type
     */
    @FunctionalInterface
    protected interface DiskStoreFactory<T extends OpenBsdHWDiskStore> {
        T create(String name, String model, String serial, long size);
    }
}
