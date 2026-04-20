/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.tuples.Pair;

/**
 * Common Windows hard disk implementation shared between JNA and FFM.
 */
@ThreadSafe
public abstract class WindowsHWDiskStore extends AbstractHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStore.class);

    protected static final String PHYSICALDRIVE_PREFIX = "\\\\.\\PHYSICALDRIVE";
    protected static final Pattern DEVICE_ID = Pattern.compile(".*\\.DeviceID=\"(.*)\"");

    protected static final int GUID_BUFSIZE = 100;
    protected static final int LABEL_BUFSIZE = 33;

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList = Collections.emptyList();

    protected WindowsHWDiskStore(String name, String model, String serial, long size) {
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

    /**
     * Sets the partition list for this disk.
     *
     * @param partitionList the partition list
     */
    protected void setPartitionList(List<HWPartition> partitionList) {
        this.partitionList = partitionList;
    }

    /**
     * Sets all disk statistics from a DiskStats object for the given index.
     *
     * @param stats the disk stats
     * @param index the disk index key
     */
    protected void setDiskStats(DiskStats stats, String index) {
        this.reads = stats.getReadMap().getOrDefault(index, 0L);
        this.readBytes = stats.getReadByteMap().getOrDefault(index, 0L);
        this.writes = stats.getWriteMap().getOrDefault(index, 0L);
        this.writeBytes = stats.getWriteByteMap().getOrDefault(index, 0L);
        this.currentQueueLength = stats.getQueueLengthMap().getOrDefault(index, 0L);
        this.transferTime = stats.getDiskTimeMap().getOrDefault(index, 0L);
        this.timeStamp = stats.getTimeStamp();
    }

    @Override
    public boolean updateAttributes() {
        String index = null;
        List<HWPartition> partitions = getPartitions();
        if (!partitions.isEmpty()) {
            index = Integer.toString(partitions.get(0).getMajor());
        } else if (getName().startsWith(PHYSICALDRIVE_PREFIX)) {
            index = getName().substring(PHYSICALDRIVE_PREFIX.length());
        } else {
            LOG.warn("Couldn't match index for {}", getName());
            return false;
        }
        DiskStats stats = queryReadWriteStats(index);
        if (stats.getReadMap().containsKey(index)) {
            setDiskStats(stats, index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Queries performance counter data for disk read/write stats.
     *
     * @param index The disk index to query, or null for all disks
     * @return A non-null {@link DiskStats} object; its maps (e.g. {@link DiskStats#getReadMap()}) are never null but
     *         may be empty if no counters matched the given index
     */
    protected abstract DiskStats queryReadWriteStats(String index);

    /**
     * Populates a DiskStats object from performance counter data.
     *
     * @param index             The disk index to filter on, or null for all
     * @param instanceValuePair The raw PDH counter data
     * @return A populated DiskStats object
     */
    protected static DiskStats populateDiskStats(String index,
            Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> instanceValuePair) {
        DiskStats stats = new DiskStats();
        List<String> instances = instanceValuePair.getA();
        Map<PhysicalDiskProperty, List<Long>> valueMap = instanceValuePair.getB();
        stats.setTimeStamp(System.currentTimeMillis());
        List<Long> readList = valueMap.get(PhysicalDiskProperty.DISKREADSPERSEC);
        List<Long> readByteList = valueMap.get(PhysicalDiskProperty.DISKREADBYTESPERSEC);
        List<Long> writeList = valueMap.get(PhysicalDiskProperty.DISKWRITESPERSEC);
        List<Long> writeByteList = valueMap.get(PhysicalDiskProperty.DISKWRITEBYTESPERSEC);
        List<Long> queueLengthList = valueMap.get(PhysicalDiskProperty.CURRENTDISKQUEUELENGTH);
        List<Long> diskTimeList = valueMap.get(PhysicalDiskProperty.PERCENTDISKTIME);

        if (instances.isEmpty() || readList == null || readByteList == null || writeList == null
                || writeByteList == null || queueLengthList == null || diskTimeList == null) {
            LOG.debug("Empty disk stats for index {}: instances={}, counters null: r={} rb={} w={} wb={} ql={} dt={}",
                    index, instances.isEmpty(), readList == null, readByteList == null, writeList == null,
                    writeByteList == null, queueLengthList == null, diskTimeList == null);
            return stats;
        }
        for (int i = 0; i < instances.size(); i++) {
            String name = getIndexFromName(instances.get(i));
            if (index != null && !index.equals(name)) {
                continue;
            }
            stats.getReadMap().put(name, readList.get(i));
            stats.getReadByteMap().put(name, readByteList.get(i));
            stats.getWriteMap().put(name, writeList.get(i));
            stats.getWriteByteMap().put(name, writeByteList.get(i));
            stats.getQueueLengthMap().put(name, queueLengthList.get(i));
            stats.getDiskTimeMap().put(name, diskTimeList.get(i) / 10_000L);
        }
        return stats;
    }

    /**
     * Populates the drive-to-partition map from WMI association data.
     *
     * @param maps       The PartitionMaps to populate
     * @param antecedent The antecedent device ID string
     * @param dependent  The dependent device ID string
     */
    protected static void mapDriveToPartition(PartitionMaps maps, String antecedent, String dependent) {
        Matcher mAnt = DEVICE_ID.matcher(antecedent);
        Matcher mDep = DEVICE_ID.matcher(dependent);
        if (mAnt.matches() && mDep.matches()) {
            maps.getDriveToPartitionMap().computeIfAbsent(mAnt.group(1).replace("\\\\", "\\"), x -> new ArrayList<>())
                    .add(mDep.group(1));
        }
    }

    /**
     * Populates the partition-to-logical-drive map from WMI association data.
     *
     * @param maps       The PartitionMaps to populate
     * @param antecedent The antecedent device ID string
     * @param dependent  The dependent device ID string
     * @param size       The partition size
     */
    protected static void mapPartitionToLogicalDrive(PartitionMaps maps, String antecedent, String dependent,
            long size) {
        Matcher mAnt = DEVICE_ID.matcher(antecedent);
        Matcher mDep = DEVICE_ID.matcher(dependent);
        if (mAnt.matches() && mDep.matches()) {
            maps.getPartitionToLogicalDriveMap().computeIfAbsent(mAnt.group(1), x -> new ArrayList<>())
                    .add(new Pair<>(mDep.group(1) + "\\", size));
        }
    }

    /**
     * Builds an unmodifiable sorted partition list from the maps.
     *
     * @param maps     The partition maps
     * @param diskName The disk name to look up
     * @return A sorted unmodifiable list of partitions
     */
    protected static List<HWPartition> buildPartitionList(PartitionMaps maps, String diskName) {
        List<HWPartition> partitions = new ArrayList<>();
        List<String> partList = maps.getDriveToPartitionMap().get(diskName);
        if (partList != null && !partList.isEmpty()) {
            for (String part : partList) {
                if (maps.getPartitionMap().containsKey(part)) {
                    partitions.addAll(maps.getPartitionMap().get(part));
                }
            }
        }
        return Collections.unmodifiableList(
                partitions.stream().sorted(Comparator.comparing(HWPartition::getName)).collect(Collectors.toList()));
    }

    /**
     * Parse a drive name like "0 C:" to just the index "0"
     *
     * @param s A drive name to parse
     * @return The first space-delimited value
     */
    protected static String getIndexFromName(String s) {
        return s.split("\\s", 2)[0];
    }

    /**
     * Maps to store read/write bytes per drive index
     */
    protected static final class DiskStats {
        private final Map<String, Long> readMap = new HashMap<>();
        private final Map<String, Long> readByteMap = new HashMap<>();
        private final Map<String, Long> writeMap = new HashMap<>();
        private final Map<String, Long> writeByteMap = new HashMap<>();
        private final Map<String, Long> queueLengthMap = new HashMap<>();
        private final Map<String, Long> diskTimeMap = new HashMap<>();
        private long timeStamp;

        public DiskStats() {
        }

        public Map<String, Long> getReadMap() {
            return readMap;
        }

        public Map<String, Long> getReadByteMap() {
            return readByteMap;
        }

        public Map<String, Long> getWriteMap() {
            return writeMap;
        }

        public Map<String, Long> getWriteByteMap() {
            return writeByteMap;
        }

        public Map<String, Long> getQueueLengthMap() {
            return queueLengthMap;
        }

        public Map<String, Long> getDiskTimeMap() {
            return diskTimeMap;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }
    }

    /**
     * Maps for the partition structure
     */
    protected static final class PartitionMaps {
        private final Map<String, List<String>> driveToPartitionMap = new HashMap<>();
        private final Map<String, List<Pair<String, Long>>> partitionToLogicalDriveMap = new HashMap<>();
        private final Map<String, List<HWPartition>> partitionMap = new HashMap<>();

        public PartitionMaps() {
        }

        public Map<String, List<String>> getDriveToPartitionMap() {
            return driveToPartitionMap;
        }

        public Map<String, List<Pair<String, Long>>> getPartitionToLogicalDriveMap() {
            return partitionToLogicalDriveMap;
        }

        public Map<String, List<HWPartition>> getPartitionMap() {
            return partitionMap;
        }
    }
}
