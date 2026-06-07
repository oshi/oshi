/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.freebsd.disk.GeomDiskList;
import oshi.driver.common.unix.freebsd.disk.GeomPartList;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdHWDiskStore;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;
import oshi.util.tuples.Triplet;

/**
 * FreeBSD hard disk implementation.
 */
@ThreadSafe
public final class FreeBsdHWDiskStoreJNA extends FreeBsdHWDiskStore {

    private FreeBsdHWDiskStoreJNA(String name, String model, String serial, long size) {
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
            setReads((long) ParseUtil.parseDoubleOrDefault(split[1], 0d));
            setWrites((long) ParseUtil.parseDoubleOrDefault(split[2], 0d));
            // In KB
            setReadBytes((long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024));
            setWriteBytes((long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024));
            // # transactions
            setCurrentQueueLength(ParseUtil.parseLongOrDefault(split[5], 0L));
            // In seconds, multiply for ms
            setTransferTime((long) (ParseUtil.parseDoubleOrDefault(split[6], 0d) * 1000));
            setTimeStamp(now);
        }
        return diskFound;
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        // Result
        List<HWDiskStore> diskList = new ArrayList<>();

        // Get map of disk names to partitions
        Map<String, List<HWPartition>> partitionMap = GeomPartList.queryPartitions();

        // Get map of disk names to disk info
        Map<String, Triplet<String, String, Long>> diskInfoMap = GeomDiskList.queryDisks();

        // Get list of disks from sysctl
        List<String> devices = Arrays.asList(ParseUtil.whitespaces.split(BsdSysctlUtil.sysctl("kern.disks", "")));

        // Run iostat -Ix to enumerate disks by name and get kb r/w
        List<String> iostat = ExecutingCommand.runNative("iostat -Ix");
        long now = System.currentTimeMillis();
        for (String line : iostat) {
            String[] split = ParseUtil.whitespaces.split(line);
            if (split.length > 6 && devices.contains(split[0])) {
                Triplet<String, String, Long> storeInfo = diskInfoMap.get(split[0]);
                FreeBsdHWDiskStoreJNA store = (storeInfo == null)
                        ? new FreeBsdHWDiskStoreJNA(split[0], Constants.UNKNOWN, Constants.UNKNOWN, 0L)
                        : new FreeBsdHWDiskStoreJNA(split[0], storeInfo.getA(), storeInfo.getB(), storeInfo.getC());
                store.setReads((long) ParseUtil.parseDoubleOrDefault(split[1], 0d));
                store.setWrites((long) ParseUtil.parseDoubleOrDefault(split[2], 0d));
                // In KB
                store.setReadBytes((long) (ParseUtil.parseDoubleOrDefault(split[3], 0d) * 1024));
                store.setWriteBytes((long) (ParseUtil.parseDoubleOrDefault(split[4], 0d) * 1024));
                // # transactions
                store.setCurrentQueueLength(ParseUtil.parseLongOrDefault(split[5], 0L));
                // In seconds, multiply for ms
                store.setTransferTime((long) (ParseUtil.parseDoubleOrDefault(split[6], 0d) * 1000));
                store.setPartitionList(Collections
                        .unmodifiableList(partitionMap.getOrDefault(split[0], Collections.emptyList()).stream()
                                .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
                store.setTimeStamp(now);
                diskList.add(store);
            }
        }
        return diskList;
    }
}
