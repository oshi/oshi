/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sun.jna.Native;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_disk_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.aix.Ls;
import oshi.driver.unix.aix.Lscfg;
import oshi.driver.unix.aix.Lspv;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.Constants;
import oshi.util.tuples.Pair;

/**
 * AIX hard disk implementation.
 */
@ThreadSafe
public final class AixHWDiskStore extends AbstractHWDiskStore {

    private final Supplier<perfstat_disk_t[]> diskStats;

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private AixHWDiskStore(String name, String model, String serial, long size, Supplier<perfstat_disk_t[]> diskStats) {
        super(name, model, serial, size);
        this.diskStats = diskStats;
    }

    @Override
    public synchronized long getReads() {
        return reads;
    }

    @Override
    public synchronized long getReadBytes() {
        return readBytes;
    }

    @Override
    public synchronized long getWrites() {
        return writes;
    }

    @Override
    public synchronized long getWriteBytes() {
        return writeBytes;
    }

    @Override
    public synchronized long getCurrentQueueLength() {
        return currentQueueLength;
    }

    @Override
    public synchronized long getTransferTime() {
        return transferTime;
    }

    @Override
    public synchronized long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public List<HWPartition> getPartitions() {
        return this.partitionList;
    }

    @Override
    public synchronized boolean updateAttributes() {
        long now = System.currentTimeMillis();
        for (perfstat_disk_t stat : diskStats.get()) {
            String name = Native.toString(stat.name);
            if (name.equals(this.getName())) {
                // we only have total transfers so estimate read/write ratio from blocks
                long blks = stat.rblks + stat.wblks;
                if (blks == 0L) {
                    this.reads = stat.xfers;
                    this.writes = 0L;
                } else {
                    long approximateReads = Math.round(stat.xfers * stat.rblks / (double) blks);
                    long approximateWrites = stat.xfers - approximateReads;
                    // Enforce monotonic increase
                    if (approximateReads > this.reads) {
                        this.reads = approximateReads;
                    }
                    if (approximateWrites > this.writes) {
                        this.writes = approximateWrites;
                    }
                }
                this.readBytes = stat.rblks * stat.bsize;
                this.writeBytes = stat.wblks * stat.bsize;
                this.currentQueueLength = stat.qdepth;
                this.transferTime = stat.time;
                this.timeStamp = now;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the disks on this machine
     *
     * @param diskStats Memoized supplier of disk statistics
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks(Supplier<perfstat_disk_t[]> diskStats) {
        Map<String, Pair<Integer, Integer>> majMinMap = Ls.queryDeviceMajorMinor();
        List<AixHWDiskStore> storeList = new ArrayList<>();
        for (perfstat_disk_t disk : diskStats.get()) {
            String storeName = Native.toString(disk.name);
            Pair<String, String> ms = Lscfg.queryModelSerial(storeName);
            String model = ms.getA() == null ? Native.toString(disk.description) : ms.getA();
            String serial = ms.getB() == null ? Constants.UNKNOWN : ms.getB();
            storeList.add(createStore(storeName, model, serial, disk.size << 20, diskStats, majMinMap));
        }
        return storeList.stream()
                .sorted(Comparator.comparingInt(
                        s -> s.getPartitions().isEmpty() ? Integer.MAX_VALUE : s.getPartitions().get(0).getMajor()))
                .collect(Collectors.toList());
    }

    private static AixHWDiskStore createStore(String diskName, String model, String serial, long size,
            Supplier<perfstat_disk_t[]> diskStats, Map<String, Pair<Integer, Integer>> majMinMap) {
        AixHWDiskStore store = new AixHWDiskStore(diskName, model.isEmpty() ? Constants.UNKNOWN : model, serial, size,
                diskStats);
        store.partitionList = Lspv.queryLogicalVolumes(diskName, majMinMap);
        store.updateAttributes();
        return store;
    }
}
