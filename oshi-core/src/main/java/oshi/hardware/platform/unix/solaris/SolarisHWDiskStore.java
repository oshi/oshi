/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import static oshi.software.os.unix.solaris.SolarisOperatingSystem.HAS_KSTAT2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;
import com.sun.jna.platform.unix.solaris.LibKstat.KstatIO;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.solaris.disk.Iostat;
import oshi.driver.unix.solaris.disk.Lshal;
import oshi.driver.unix.solaris.disk.Prtvtoc;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;
import oshi.util.tuples.Quintet;

/**
 * Solaris hard disk implementation.
 */
@ThreadSafe
public final class SolarisHWDiskStore extends AbstractHWDiskStore {

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList;

    private SolarisHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    public long getReads() {
        return reads;
    }

    @Override
    public long getReadBytes() {
        return readBytes;
    }

    @Override
    public long getWrites() {
        return writes;
    }

    @Override
    public long getWriteBytes() {
        return writeBytes;
    }

    @Override
    public long getCurrentQueueLength() {
        return currentQueueLength;
    }

    @Override
    public long getTransferTime() {
        return transferTime;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public List<HWPartition> getPartitions() {
        return this.partitionList;
    }

    @Override
    public boolean updateAttributes() {
        this.timeStamp = System.currentTimeMillis();
        if (HAS_KSTAT2) {
            // Use Kstat2 implementation
            return updateAttributes2();
        }
        try (KstatChain kc = KstatUtil.openChain()) {
            Kstat ksp = kc.lookup(null, 0, getName());
            if (ksp != null && kc.read(ksp)) {
                KstatIO data = new KstatIO(ksp.ks_data);
                this.reads = data.reads;
                this.writes = data.writes;
                this.readBytes = data.nread;
                this.writeBytes = data.nwritten;
                this.currentQueueLength = (long) data.wcnt + data.rcnt;
                // rtime and snaptime are nanoseconds, convert to millis
                this.transferTime = data.rtime / 1_000_000L;
                this.timeStamp = ksp.ks_snaptime / 1_000_000L;
                return true;
            }
        }
        return false;
    }

    private boolean updateAttributes2() {
        String fullName = getName();
        String alpha = fullName;
        String numeric = "";
        for (int c = 0; c < fullName.length(); c++) {
            if (fullName.charAt(c) >= '0' && fullName.charAt(c) <= '9') {
                alpha = fullName.substring(0, c);
                numeric = fullName.substring(c);
                break;
            }
        }
        // Try device style notation
        Object[] results = KstatUtil.queryKstat2("kstat:/disk/" + alpha + "/" + getName() + "/0", "reads", "writes",
                "nread", "nwritten", "wcnt", "rcnt", "rtime", "snaptime");
        // If failure try io notation
        if (results[results.length - 1] == null) {
            results = KstatUtil.queryKstat2("kstat:/disk/" + alpha + "/" + numeric + "/io", "reads", "writes", "nread",
                    "nwritten", "wcnt", "rcnt", "rtime", "snaptime");
        }
        if (results[results.length - 1] == null) {
            return false;
        }
        this.reads = results[0] == null ? 0L : (long) results[0];
        this.writes = results[1] == null ? 0L : (long) results[1];
        this.readBytes = results[2] == null ? 0L : (long) results[2];
        this.writeBytes = results[3] == null ? 0L : (long) results[3];
        this.currentQueueLength = results[4] == null ? 0L : (long) results[4];
        this.currentQueueLength += results[5] == null ? 0L : (long) results[5];
        // rtime and snaptime are nanoseconds, convert to millis
        this.transferTime = results[6] == null ? 0L : (long) results[6] / 1_000_000L;
        this.timeStamp = (long) results[7] / 1_000_000L;
        return true;
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        // Create map to correlate disk name with block device mount point for
        // later use in partition info
        Map<String, String> deviceMap = Iostat.queryPartitionToMountMap();

        // Create map to correlate disk name with block device mount point for
        // later use in partition info. Run lshal, if available, to get block device
        // major (we'll use partition # for minor)
        Map<String, Integer> majorMap = Lshal.queryDiskToMajorMap();

        // Create map of model, vendor, product, serial, size
        // We'll use Model if available, otherwise Vendor+Product
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

    private static SolarisHWDiskStore createStore(String diskName, String model, String vendor, String product,
            String serial, long size, String mount, int major) {
        SolarisHWDiskStore store = new SolarisHWDiskStore(diskName,
                model.isEmpty() ? (vendor + " " + product).trim() : model, serial, size);
        store.partitionList = Collections.unmodifiableList(Prtvtoc.queryPartitions(mount, major).stream()
                .sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
        store.updateAttributes();
        return store;
    }
}
