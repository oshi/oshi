/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.Ls;
import oshi.driver.common.unix.aix.Lscfg;
import oshi.driver.common.unix.aix.Lspv;
import oshi.ffm.driver.unix.aix.perfstat.PerfstatDiskFFM;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.aix.AixHWDiskStore;
import oshi.util.Constants;
import oshi.util.tuples.Pair;

/**
 * FFM-backed AIX HWDiskStore.
 */
@ThreadSafe
public final class AixHWDiskStoreFFM extends AixHWDiskStore {

    private final Supplier<PerfstatDiskFFM.Disk[]> diskStats;

    private AixHWDiskStoreFFM(String name, String model, String serial, long size,
            Supplier<PerfstatDiskFFM.Disk[]> diskStats) {
        super(name, model, serial, size);
        this.diskStats = diskStats;
    }

    @Override
    protected DiskStats queryStats() {
        for (PerfstatDiskFFM.Disk stat : diskStats.get()) {
            if (stat.name.equals(this.getName())) {
                DiskStats out = new DiskStats();
                out.xfers = stat.xfers;
                out.rblks = stat.rblks;
                out.wblks = stat.wblks;
                out.bsize = stat.bsize;
                out.qdepth = stat.qdepth;
                out.time = stat.time;
                return out;
            }
        }
        return null;
    }

    /**
     * Gets the disks on this machine.
     *
     * @param diskStats memoized supplier of disk statistics
     * @return a list of {@link HWDiskStore} objects
     */
    public static List<HWDiskStore> getDisks(Supplier<PerfstatDiskFFM.Disk[]> diskStats) {
        Map<String, Pair<Integer, Integer>> majMinMap = Ls.queryDeviceMajorMinor();
        List<AixHWDiskStoreFFM> storeList = new ArrayList<>();
        for (PerfstatDiskFFM.Disk disk : diskStats.get()) {
            String storeName = disk.name;
            Pair<String, String> ms = Lscfg.queryModelSerial(storeName);
            String model = ms.getA() == null ? disk.description : ms.getA();
            String serial = ms.getB() == null ? Constants.UNKNOWN : ms.getB();
            storeList.add(createStore(storeName, model, serial, disk.size << 20, diskStats, majMinMap));
        }
        return storeList.stream()
                .sorted(Comparator.comparingInt(
                        s -> s.getPartitions().isEmpty() ? Integer.MAX_VALUE : s.getPartitions().get(0).getMajor()))
                .collect(Collectors.toList());
    }

    private static AixHWDiskStoreFFM createStore(String diskName, String model, String serial, long size,
            Supplier<PerfstatDiskFFM.Disk[]> diskStats, Map<String, Pair<Integer, Integer>> majMinMap) {
        AixHWDiskStoreFFM store = new AixHWDiskStoreFFM(diskName, model.isEmpty() ? Constants.UNKNOWN : model, serial,
                size, diskStats);
        store.setPartitionList(Lspv.queryLogicalVolumes(diskName, majMinMap));
        store.updateAttributes();
        return store;
    }
}
