/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

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
import oshi.util.platform.linux.DevPath;
import oshi.util.platform.linux.ProcPath;

/**
 * Linux hard disk implementation.
 */
@ThreadSafe
public abstract class LinuxHWDiskStore extends AbstractHWDiskStore {

    protected static final String BLOCK = "block";
    protected static final String DISK = "disk";
    protected static final String PARTITION = "partition";

    protected static final String STAT = "stat";
    protected static final String SIZE = "size";
    protected static final String MINOR = "MINOR";
    protected static final String MAJOR = "MAJOR";

    protected static final String ID_FS_TYPE = "ID_FS_TYPE";
    protected static final String ID_FS_UUID = "ID_FS_UUID";
    protected static final String ID_FS_LABEL = "ID_FS_LABEL";
    protected static final String ID_MODEL = "ID_MODEL";
    protected static final String ID_SERIAL_SHORT = "ID_SERIAL_SHORT";

    protected static final String DM_UUID = "DM_UUID";
    protected static final String DM_VG_NAME = "DM_VG_NAME";
    protected static final String DM_LV_NAME = "DM_LV_NAME";
    protected static final String LOGICAL_VOLUME_GROUP = "Logical Volume Group";

    protected static final int SECTORSIZE = 512;

    // Get a list of orders to pass to ParseUtil
    protected static final int[] UDEV_STAT_ORDERS = new int[UdevStat.values().length];
    static {
        for (UdevStat stat : UdevStat.values()) {
            UDEV_STAT_ORDERS[stat.ordinal()] = stat.getOrder();
        }
    }

    // There are at least 11 elements in udev stat output or sometimes 15. We want
    // the rightmost 11 or 15 if there is leading text.
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

    protected LinuxHWDiskStore(String name, String model, String serial, long size) {
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

    protected void setPartitionList(List<HWPartition> partitionList) {
        this.partitionList = partitionList;
    }

    protected List<HWPartition> getMutablePartitionList() {
        return this.partitionList;
    }

    protected static void computeDiskStats(LinuxHWDiskStore store, String devstat) {
        long[] devstatArray = ParseUtil.parseStringToLongArray(devstat, UDEV_STAT_ORDERS, UDEV_STAT_LENGTH, ' ');
        store.setDiskStats(devstatArray[UdevStat.READS.ordinal()],
                devstatArray[UdevStat.READ_BYTES.ordinal()] * SECTORSIZE, devstatArray[UdevStat.WRITES.ordinal()],
                devstatArray[UdevStat.WRITE_BYTES.ordinal()] * SECTORSIZE,
                devstatArray[UdevStat.QUEUE_LENGTH.ordinal()], devstatArray[UdevStat.ACTIVE_MS.ordinal()],
                System.currentTimeMillis());
    }

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

    protected static String getPartitionNameForDmDevice(String vgName, String lvName) {
        return DevPath.DEV + vgName + '/' + lvName;
    }

    protected static String getMountPointOfDmDevice(String vgName, String lvName) {
        return DevPath.MAPPER + vgName + '-' + lvName;
    }

    protected static String getDependentNamesFromHoldersDirectory(String sysPath) {
        File holdersDir = new File(sysPath + "/holders");
        File[] holders = holdersDir.listFiles();
        if (holders != null) {
            return Arrays.stream(holders).map(File::getName).collect(Collectors.joining(" "));
        }
        return "";
    }

    protected static void finalizePartitions(List<HWDiskStore> result) {
        for (HWDiskStore hwds : result) {
            LinuxHWDiskStore store = (LinuxHWDiskStore) hwds;
            store.setPartitionList(Collections.unmodifiableList(store.getPartitions().stream()
                    .sorted(java.util.Comparator.comparing(HWPartition::getName)).collect(Collectors.toList())));
        }
    }

    // Order the field is in udev stats
    protected enum UdevStat {
        // The parsing implementation in ParseUtil requires these to be declared
        // in increasing order. Use 0-ordered index here
        READS(0), READ_BYTES(2), WRITES(4), WRITE_BYTES(6), QUEUE_LENGTH(8), ACTIVE_MS(9);

        private int order;

        public int getOrder() {
            return this.order;
        }

        UdevStat(int order) {
            this.order = order;
        }
    }
}
