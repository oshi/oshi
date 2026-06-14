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
import oshi.util.Util;
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
    /** Device-mapper name property. */
    protected static final String DM_NAME = "DM_NAME";
    /** Device-mapper volume group name property. */
    protected static final String DM_VG_NAME = "DM_VG_NAME";
    /** Device-mapper logical volume name property. */
    protected static final String DM_LV_NAME = "DM_LV_NAME";
    /** Logical volume group description string. */
    protected static final String LOGICAL_VOLUME_GROUP = "Logical Volume Group";
    /** Encrypted volume description string. */
    protected static final String ENCRYPTED_VOLUME = "Encrypted Volume";
    /** Device-mapper description string. */
    protected static final String DEVICE_MAPPER = "Device Mapper";

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
        setPartitionList(new ArrayList<>());
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
        setPartitionList(new ArrayList<>());
    }

    /**
     * Gets the mutable partition list for building.
     *
     * @return the mutable partition list
     */
    protected List<HWPartition> getMutablePartitionList() {
        return getPartitions();
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
     * Gets the model description for a device-mapper device.
     *
     * @param dmUuid the device-mapper UUID
     * @return the model description
     */
    protected static String getModelForDmDevice(String dmUuid) {
        if (isLogicalVolume(dmUuid)) {
            return LOGICAL_VOLUME_GROUP;
        } else if (isEncryptedVolume(dmUuid)) {
            return ENCRYPTED_VOLUME;
        }
        return DEVICE_MAPPER;
    }

    /**
     * Checks whether a device-mapper UUID identifies an LVM logical volume.
     *
     * @param dmUuid the device-mapper UUID
     * @return whether the UUID identifies an LVM logical volume
     */
    protected static boolean isLogicalVolume(String dmUuid) {
        return dmUuid != null && dmUuid.startsWith("LVM-");
    }

    /**
     * Checks whether a device-mapper UUID identifies an encrypted volume.
     *
     * @param dmUuid the device-mapper UUID
     * @return whether the UUID identifies an encrypted volume
     */
    protected static boolean isEncryptedVolume(String dmUuid) {
        return dmUuid != null && dmUuid.startsWith("CRYPT-");
    }

    /**
     * Gets the preferred path for a device-mapper device.
     *
     * @param dmName  the device-mapper name
     * @param devnode the device node path
     * @return the device path
     */
    protected static String getDmDevicePath(String dmName, String devnode) {
        return Util.isBlank(dmName) ? devnode : DevPath.MAPPER + dmName;
    }

    /**
     * Gets the mount point for a device-mapper device.
     *
     * @param mountsMap the map of device paths to mount points
     * @param dmName    the device-mapper name
     * @param devnode   the device node path
     * @param sysPath   the sysfs path for the device
     * @return the mount point or dependent device names
     */
    protected static String getMountPointForDmDevice(Map<String, String> mountsMap, String dmName, String devnode,
            String sysPath) {
        String devicePath = getDmDevicePath(dmName, devnode);
        String mountPoint = mountsMap.get(devicePath);
        if (mountPoint != null) {
            return mountPoint;
        }
        if (!devicePath.equals(devnode)) {
            mountPoint = mountsMap.get(devnode);
            if (mountPoint != null) {
                return mountPoint;
            }
        }
        return getDependentNamesFromHoldersDirectory(sysPath);
    }

    /**
     * Adds a partition entry for a supported device-mapper device.
     *
     * @param store     the disk store to update
     * @param mountsMap the map of device paths to mount points
     * @param dmUuid    the device-mapper UUID
     * @param vgName    the LVM volume group name
     * @param lvName    the LVM logical volume name
     * @param dmName    the device-mapper name
     * @param devnode   the device node path
     * @param sysname   the sysfs device name
     * @param sysPath   the sysfs path for the device
     * @param fsType    the filesystem type
     * @param fsUuid    the filesystem UUID
     * @param fsLabel   the filesystem label
     * @param size      the partition size in bytes
     * @param major     the major device ID
     * @param minor     the minor device ID
     */
    protected static void addDeviceMapperPartition(LinuxHWDiskStore store, Map<String, String> mountsMap, String dmUuid,
            String vgName, String lvName, String dmName, String devnode, String sysname, String sysPath, String fsType,
            String fsUuid, String fsLabel, long size, int major, int minor) {
        if (isLogicalVolume(dmUuid) && !Util.isBlank(vgName) && !Util.isBlank(lvName)) {
            store.getMutablePartitionList().add(new HWPartition(getPartitionNameForDmDevice(vgName, lvName), sysname,
                    fsType == null ? PARTITION : fsType, fsUuid == null ? "" : fsUuid, fsLabel == null ? "" : fsLabel,
                    size, major, minor, getMountPointOfDmDevice(vgName, lvName)));
        } else if (isEncryptedVolume(dmUuid)) {
            String name = getDmDevicePath(dmName, devnode);
            store.getMutablePartitionList()
                    .add(new HWPartition(name, sysname, fsType == null ? PARTITION : fsType,
                            fsUuid == null ? "" : fsUuid, fsLabel == null ? "" : fsLabel, size, major, minor,
                            getMountPointForDmDevice(mountsMap, dmName, devnode, sysPath)));
        }
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
