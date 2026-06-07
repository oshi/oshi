/*
 * Copyright 2020-2026 The OSHI Project Contributors
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
import oshi.driver.common.unix.aix.Ls;
import oshi.driver.common.unix.aix.Lscfg;
import oshi.driver.common.unix.aix.Lspv;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.Constants;
import oshi.util.tuples.Pair;

/**
 * AIX hard disk implementation.
 */
@ThreadSafe
public final class AixHWDiskStore extends AbstractHWDiskStore {

    private final Supplier<perfstat_disk_t[]> diskStats;

    private AixHWDiskStore(String name, String model, String serial, long size, Supplier<perfstat_disk_t[]> diskStats) {
        super(name, model, serial, size);
        this.diskStats = diskStats;
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
                    setReads(stat.xfers);
                    setWrites(0L);
                } else {
                    long approximateReads = Math.round(stat.xfers * stat.rblks / (double) blks);
                    long approximateWrites = stat.xfers - approximateReads;
                    // Enforce monotonic increase
                    if (approximateReads > getReads()) {
                        setReads(approximateReads);
                    }
                    if (approximateWrites > getWrites()) {
                        setWrites(approximateWrites);
                    }
                }
                setReadBytes(stat.rblks * stat.bsize);
                setWriteBytes(stat.wblks * stat.bsize);
                setCurrentQueueLength(stat.qdepth);
                setTransferTime(stat.time);
                setTimeStamp(now);
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
        store.setPartitionList(Lspv.queryLogicalVolumes(diskName, majMinMap));
        store.updateAttributes();
        return store;
    }
}
