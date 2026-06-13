/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.freebsd.disk.GeomDiskList;
import oshi.driver.common.unix.freebsd.disk.GeomPartList;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Shared FreeBSD HWDiskStore logic. Subclasses provide the sysctl backend (JNA or FFM).
 */
@ThreadSafe
public abstract class FreeBsdHWDiskStore extends AbstractHWDiskStore {

    protected FreeBsdHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public boolean updateAttributes() {
        List<String> output = ExecutingCommand.runNative("iostat -Ix " + getName());
        long now = System.currentTimeMillis();
        boolean diskFound = false;
        for (String line : output) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length < 7 || !split[0].equals(getName())) {
                continue;
            }
            diskFound = true;
            this.reads = (long) ParseUtil.parseDoubleOrDefault(split[1], 0d);
            this.writes = (long) ParseUtil.parseDoubleOrDefault(split[2], 0d);
            this.readBytes = (long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024);
            this.writeBytes = (long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024);
            this.currentQueueLength = ParseUtil.parseLongOrDefault(split[5], 0L);
            this.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[6], 0d) * 1000);
            this.timeStamp = now;
        }
        return diskFound;
    }

    /**
     * Gets the disks on this machine using the provided sysctl accessor.
     *
     * @param <T>       the concrete disk store type
     * @param sysctlStr reads a string sysctl given (name, default)
     * @param factory   constructs the platform-specific instance
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    protected static <T extends FreeBsdHWDiskStore> List<HWDiskStore> getDisks(
            BiFunction<String, String, String> sysctlStr, DiskStoreFactory<T> factory) {
        List<HWDiskStore> diskList = new ArrayList<>();

        Map<String, List<HWPartition>> partitionMap = GeomPartList.queryPartitions();
        Map<String, Triplet<String, String, Long>> diskInfoMap = GeomDiskList.queryDisks();

        String kernDisks = sysctlStr.apply("kern.disks", "");
        if (kernDisks.isEmpty()) {
            return diskList;
        }
        List<String> devices = Arrays.asList(ParseUtil.whitespaces.split(kernDisks));

        List<String> iostat = ExecutingCommand.runNative("iostat -Ix");
        long now = System.currentTimeMillis();
        for (String line : iostat) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 6 && devices.contains(split[0])) {
                Triplet<String, String, Long> storeInfo = diskInfoMap.get(split[0]);
                T store = (storeInfo == null) ? factory.create(split[0], Constants.UNKNOWN, Constants.UNKNOWN, 0L)
                        : factory.create(split[0], storeInfo.getA(), storeInfo.getB(), storeInfo.getC());
                store.reads = (long) ParseUtil.parseDoubleOrDefault(split[1], 0d);
                store.writes = (long) ParseUtil.parseDoubleOrDefault(split[2], 0d);
                store.readBytes = (long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024);
                store.writeBytes = (long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024);
                store.currentQueueLength = ParseUtil.parseLongOrDefault(split[5], 0L);
                store.transferTime = (long) (ParseUtil.parseDoubleOrDefault(split[6], 0d) * 1000);
                store.setPartitionList(
                        Collections.unmodifiableList(partitionMap.getOrDefault(split[0], Collections.emptyList())
                                .stream().sorted(Comparator.comparing(HWPartition::getName))
                                .collect(java.util.stream.Collectors.toList())));
                store.timeStamp = now;
                diskList.add(store);
            }
        }
        return diskList;
    }

    /**
     * Factory for creating platform-specific FreeBsdHWDiskStore instances.
     *
     * @param <T> the concrete disk store type
     */
    @FunctionalInterface
    protected interface DiskStoreFactory<T extends FreeBsdHWDiskStore> {
        T create(String name, String model, String serial, long size);
    }
}
