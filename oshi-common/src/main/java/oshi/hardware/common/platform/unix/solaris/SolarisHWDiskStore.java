/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.solaris.disk.Iostat;
import oshi.driver.common.unix.solaris.disk.Lshal;
import oshi.driver.common.unix.solaris.disk.Prtvtoc;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.tuples.Quintet;

/**
 * Abstract base for Solaris HWDiskStore. The disk enumeration ({@link #getDisks}) and partition assembly are shared and
 * call the common {@code iostat}/{@code lshal}/{@code prtvtoc} drivers; the {@code kstat}-based per-disk I/O stats are
 * native and fetched by the JNA and FFM subclasses via {@link #queryStats()}.
 */
@ThreadSafe
public abstract class SolarisHWDiskStore extends AbstractHWDiskStore {

    /** Per-disk I/O stats the Solaris HAL reads. Both JNA and FFM concrete subclasses populate this. */
    public static final class DiskStats {
        public long reads;
        public long writes;
        public long readBytes;
        public long writeBytes;
        public long currentQueueLength;
        public long transferTime;
        public long timeStamp;
    }

    /** Creates a concrete (JNA or FFM) Solaris disk store. */
    @FunctionalInterface
    protected interface DiskStoreFactory {
        SolarisHWDiskStore create(String name, String model, String serial, long size);
    }

    protected SolarisHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public boolean updateAttributes() {
        setTimeStamp(System.currentTimeMillis());
        DiskStats stats = queryStats();
        if (stats == null) {
            return false;
        }
        setReads(stats.reads);
        setWrites(stats.writes);
        setReadBytes(stats.readBytes);
        setWriteBytes(stats.writeBytes);
        setCurrentQueueLength(stats.currentQueueLength);
        setTransferTime(stats.transferTime);
        setTimeStamp(stats.timeStamp);
        return true;
    }

    /**
     * Gets the disks on this machine, building each store with the given concrete factory.
     *
     * @param factory creates the platform-specific store instances
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    protected static List<HWDiskStore> getDisks(DiskStoreFactory factory) {
        // Create map to correlate disk name with block device mount point for later use in partition info
        Map<String, String> deviceMap = Iostat.queryPartitionToMountMap();

        // Run lshal, if available, to get block device major (we'll use partition # for minor)
        Map<String, Integer> majorMap = Lshal.queryDiskToMajorMap();

        // Create map of model, vendor, product, serial, size
        // We'll use Model if available, otherwise Vendor+Product
        Map<String, Quintet<String, String, String, String, Long>> deviceStringMap = Iostat
                .queryDeviceStrings(deviceMap.keySet());

        List<HWDiskStore> storeList = new ArrayList<>();
        for (Entry<String, Quintet<String, String, String, String, Long>> entry : deviceStringMap.entrySet()) {
            String storeName = entry.getKey();
            Quintet<String, String, String, String, Long> val = entry.getValue();
            storeList.add(createStore(factory, storeName, val.getA(), val.getB(), val.getC(), val.getD(), val.getE(),
                    deviceMap.getOrDefault(storeName, ""), majorMap.getOrDefault(storeName, 0)));
        }

        return storeList;
    }

    private static SolarisHWDiskStore createStore(DiskStoreFactory factory, String diskName, String model,
            String vendor, String product, String serial, long size, String mount, int major) {
        SolarisHWDiskStore store = factory.create(diskName, model.isEmpty() ? (vendor + " " + product).trim() : model,
                serial, size);
        store.setPartitionList(Collections.unmodifiableList(Prtvtoc.queryPartitions(mount, major).stream()
                .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
        store.updateAttributes();
        return store;
    }

    /**
     * Looks up this disk's per-disk I/O stats from the subclass's kstat data source.
     *
     * @return stats POJO, or {@code null} if no entry was found for this disk name
     */
    protected abstract DiskStats queryStats();
}
