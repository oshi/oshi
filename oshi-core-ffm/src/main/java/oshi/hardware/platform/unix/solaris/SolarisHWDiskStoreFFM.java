/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.MemorySegment;
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
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.ffm.util.platform.unix.solaris.LibKstatFunctions;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.tuples.Quintet;

@ThreadSafe
public final class SolarisHWDiskStoreFFM extends AbstractHWDiskStore {

    private SolarisHWDiskStoreFFM(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public boolean updateAttributes() {
        setTimeStamp(System.currentTimeMillis());
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            MemorySegment ksp = kc.lookup(null, 0, getName());
            if (ksp.address() != 0L && kc.read(ksp)) {
                MemorySegment dataPtr = LibKstatFunctions.kstatData(ksp);
                if (dataPtr.address() == 0L) {
                    return false;
                }
                MemorySegment io = dataPtr.reinterpret(LibKstatFunctions.KSTAT_IO_LAYOUT.byteSize());
                setReads(LibKstatFunctions.kstatIoReads(io));
                setWrites(LibKstatFunctions.kstatIoWrites(io));
                setReadBytes(LibKstatFunctions.kstatIoNread(io));
                setWriteBytes(LibKstatFunctions.kstatIoNwritten(io));
                setCurrentQueueLength((long) LibKstatFunctions.kstatIoWcnt(io) + LibKstatFunctions.kstatIoRcnt(io));
                setTransferTime(LibKstatFunctions.kstatIoRtime(io) / 1_000_000L);
                setTimeStamp(LibKstatFunctions.kstatSnaptime(ksp) / 1_000_000L);
                return true;
            }
        }
        return false;
    }

    public static List<HWDiskStore> getDisks() {
        Map<String, String> deviceMap = Iostat.queryPartitionToMountMap();
        Map<String, Integer> majorMap = Lshal.queryDiskToMajorMap();
        Map<String, Quintet<String, String, String, String, Long>> deviceStringMap = Iostat
                .queryDeviceStrings(deviceMap.keySet());

        List<HWDiskStore> storeList = new ArrayList<>();
        for (Entry<String, Quintet<String, String, String, String, Long>> entry : deviceStringMap.entrySet()) {
            String storeName = entry.getKey();
            Quintet<String, String, String, String, Long> val = entry.getValue();
            storeList.add(createStore(storeName, val.getA(), val.getB(), val.getC(), val.getD(), val.getE(),
                    deviceMap.getOrDefault(storeName, ""), majorMap.getOrDefault(storeName, 0)));
        }

        return storeList;
    }

    private static SolarisHWDiskStoreFFM createStore(String diskName, String model, String vendor, String product,
            String serial, long size, String mount, int major) {
        SolarisHWDiskStoreFFM store = new SolarisHWDiskStoreFFM(diskName,
                model.isEmpty() ? (vendor + " " + product).trim() : model, serial, size);
        store.setPartitionList(Collections.unmodifiableList(Prtvtoc.queryPartitions(mount, major).stream()
                .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
        store.updateAttributes();
        return store;
    }
}
