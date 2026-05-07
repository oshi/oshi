/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.AbstractHWDiskStore;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.DevPath;
import oshi.util.linux.ProcPath;

/**
 * Linux hard disk implementation.
 */
@ThreadSafe
public abstract class LinuxHWDiskStore extends AbstractHWDiskStore {

    /** Sysfs block device type. */
    protected static final String BLOCK = "block";
    /** Sysfs disk device type. */
    protected static final String DISK = "disk";
    /** Sysfs partition device type. */
    protected static final String PARTITION = "partition";

    /** Sysfs stat file name. */
    protected static final String STAT = "stat";
    /** Sysfs size file name. */
    protected static final String SIZE = "size";
    /** Udev MINOR property. */
    protected static final String MINOR = "MINOR";
    /** Udev MAJOR property. */
    protected static final String MAJOR = "MAJOR";

    /** Udev filesystem type property. */
    protected static final String ID_FS_TYPE = "ID_FS_TYPE";
    /** Udev filesystem UUID property. */
    protected static final String ID_FS_UUID = "ID_FS_UUID";
    /** Udev filesystem label property. */
    protected static final String ID_FS_LABEL = "ID_FS_LABEL";
    /** Udev model property. */
    protected static final String ID_MODEL = "ID_MODEL";
    /** Udev serial number property. */
    protected static final String ID_SERIAL_SHORT = "ID_SERIAL_SHORT";

    /** Device-mapper UUID property. */
    protected static final String DM_UUID = "DM_UUID";
    /** Device-mapper volume group name property. */
    protected static final String DM_VG_NAME = "DM_VG_NAME";
    /** Device-mapper logical volume name property. */
    protected static final String DM_LV_NAME = "DM_LV_NAME";
    /** Logical volume group description string. */
    protected static final String LOGICAL_VOLUME_GROUP = "Logical Volume Group";

    /** Sector size in bytes. */
    protected static final int SECTORSIZE = 512;

    /** Ordering array for parsing udev stat fields. */
    protected static final int[] UDEV_STAT_ORDERS = new int[UdevStat.values().length];
    static {
        for (UdevStat stat : UdevStat.values()) {
            UDEV_STAT_ORDERS[stat.ordinal()] = stat.getOrder();
        }
    }

    /** Number of fields in udev stat output. */
    protected static final int UDEV_STAT_LENGTH;
    static {
        String stat = FileUtil.getStringFromFile(ProcPath.DISKSTATS);
        int statLength = 11;
        if (!stat.isEmpty()) {
            statLength = ParseUtil.countStringToLongArray(stat, ' ');
        }
        UDEV_STAT_LENGTH = statLength;
    }

    private long reads = 0L;
    private long readBytes = 0L;
    private long writes = 0L;
    private long writeBytes = 0L;
    private long currentQueueLength = 0L;
    private long transferTime = 0L;
    private long timeStamp = 0L;
    private List<HWPartition> partitionList = new ArrayList<>();

    /**
     * Creates a LinuxHWDiskStore with unknown disk type.
     *
     * @param name   the disk name
     * @param model  the disk model
     * @param serial the serial number
     * @param size   the disk size in bytes
     */
    protected LinuxHWDiskStore(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Creates a LinuxHWDiskStore.
     *
     * @param name     the disk name
     * @param model    the disk model
     * @param serial   the serial number
     * @param size     the disk size in bytes
     * @param diskType the disk type
     */
    protected LinuxHWDiskStore(String name, String model, String serial, long size, String diskType) {
        super(name, model, serial, size, diskType);
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
     * Sets the disk statistics.
     *
     * @param reads              number of reads
     * @param readBytes          bytes read
     * @param writes             number of writes
     * @param writeBytes         bytes written
     * @param currentQueueLength current I/O queue length
     * @param transferTime       time spent on transfers in ms
     * @param timeStamp          timestamp of the measurement
     */
    protected void setDiskStats(long reads, long readBytes, long writes, long writeBytes, long currentQueueLength,
            long transferTime, long timeStamp) {
        this.reads = reads;
        this.readBytes = readBytes;
        this.writes = writes;
        this.writeBytes = writeBytes;
        this.currentQueueLength = currentQueueLength;
        this.transferTime = transferTime;
        this.timeStamp = timeStamp;
    }

    /**
     * Sets the partition list.
     *
     * @param partitionList the list of partitions
     */
    protected void setPartitionList(List<HWPartition> partitionList) {
        this.partitionList = partitionList;
    }

    /**
     * Gets the mutable partition list for building.
     *
     * @return the mutable partition list
     */
    protected List<HWPartition> getMutablePartitionList() {
        return this.partitionList;
    }

    /**
     * Computes and sets disk statistics from a stat string.
     *
     * @param store   the disk store to update
     * @param devstat the stat string from sysfs or /proc/diskstats
     */
    protected static void computeDiskStats(LinuxHWDiskStore store, String devstat) {
        long[] devstatArray = ParseUtil.parseStringToLongArray(devstat, UDEV_STAT_ORDERS, UDEV_STAT_LENGTH, ' ');
        store.setDiskStats(devstatArray[UdevStat.READS.ordinal()],
                devstatArray[UdevStat.READ_BYTES.ordinal()] * SECTORSIZE, devstatArray[UdevStat.WRITES.ordinal()],
                devstatArray[UdevStat.WRITE_BYTES.ordinal()] * SECTORSIZE,
                devstatArray[UdevStat.QUEUE_LENGTH.ordinal()], devstatArray[UdevStat.ACTIVE_MS.ordinal()],
                System.currentTimeMillis());
    }

    /**
     * Reads /proc/mounts into a map of device to mount point.
     *
     * @return a map of device paths to mount points
     */
    protected static Map<String, String> readMountsMap() {
        Map<String, String> mountsMap = new HashMap<>();
        List<String> mounts = FileUtil.readFile(ProcPath.MOUNTS);
        for (String mount : mounts) {
            String[] split = ParseUtil.whitespaces.split(mount);
            if (split.length < 2 || !split[0].startsWith(DevPath.DEV)) {
                continue;
            }
            mountsMap.put(split[0], split[1]);
        }
        return mountsMap;
    }

    /**
     * Gets the partition name for a device-mapper device.
     *
     * @param vgName the volume group name
     * @param lvName the logical volume name
     * @return the partition name path
     */
    protected static String getPartitionNameForDmDevice(String vgName, String lvName) {
        return DevPath.DEV + vgName + '/' + lvName;
    }

    /**
     * Gets the mount point path for a device-mapper device.
     *
     * @param vgName the volume group name
     * @param lvName the logical volume name
     * @return the mount point path
     */
    protected static String getMountPointOfDmDevice(String vgName, String lvName) {
        return DevPath.MAPPER + vgName + '-' + lvName;
    }

    /**
     * Gets dependent device names from the holders directory.
     *
     * @param sysPath the sysfs path for the device
     * @return space-separated holder names, or empty string
     */
    protected static String getDependentNamesFromHoldersDirectory(String sysPath) {
        File holdersDir = new File(sysPath + "/holders");
        File[] holders = holdersDir.listFiles();
        if (holders != null) {
            return Arrays.stream(holders).map(File::getName).collect(Collectors.joining(" "));
        }
        return "";
    }

    /**
     * Sorts and makes partition lists unmodifiable for all disk stores.
     *
     * @param result the list of disk stores to finalize
     */
    protected static void finalizePartitions(List<HWDiskStore> result) {
        for (HWDiskStore hwds : result) {
            LinuxHWDiskStore store = (LinuxHWDiskStore) hwds;
            store.setPartitionList(Collections.unmodifiableList(store.getPartitions().stream()
                    .sorted(java.util.Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
        }
    }

    /**
     * Field ordering in udev stat output.
     */
    protected enum UdevStat {
        /** Number of reads completed. */
        READS(0),
        /** Number of sectors read. */
        READ_BYTES(2),
        /** Number of writes completed. */
        WRITES(4),
        /** Number of sectors written. */
        WRITE_BYTES(6),
        /** Number of I/Os currently in progress. */
        QUEUE_LENGTH(8),
        /** Time spent doing I/Os in milliseconds. */
        ACTIVE_MS(9);

        private int order;

        /**
         * Gets the field order index.
         *
         * @return the order
         */
        public int getOrder() {
            return this.order;
        }

        UdevStat(int order) {
            this.order = order;
        }
    }
}
