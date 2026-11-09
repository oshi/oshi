/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.linux.LinuxHWDiskStore;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.linux.DevPath;
import oshi.util.linux.SysPath;
import oshi.util.linux.UdevData;

/**
 * Native-free Linux disk store implementation. Enumerates block devices from {@code /sys/block/} and reads stats from
 * sysfs, without requiring udev.
 */
@ThreadSafe
public final class LinuxHWDiskStoreNF extends LinuxHWDiskStore {

    LinuxHWDiskStoreNF(String name, String model, String serial, long size, String diskType) {
        super(name, model, serial, size, diskType);
    }

    /**
     * Gets disk stores on this machine.
     *
     * @return a list of {@link HWDiskStore} objects
     */
    public static List<HWDiskStore> getDisks() {
        return getDisks(SysPath.BLOCK, readMountsMap(), UdevData.RUN_UDEV_DATA);
    }

    /**
     * Gets the disk stores under a sysfs block directory, listing the same devices as the udev backends: every block
     * device except loop and RAM disks. Package-private for testing.
     *
     * @param sysBlock    the sysfs block directory, normally {@link SysPath#BLOCK}
     * @param mountsMap   the map of device paths to mount points
     * @param udevDataDir the udev database directory, normally {@link UdevData#RUN_UDEV_DATA}
     * @return a list of {@link HWDiskStore} objects
     */
    static List<HWDiskStore> getDisks(String sysBlock, Map<String, String> mountsMap, String udevDataDir) {
        List<HWDiskStore> result = new ArrayList<>();
        File[] blockDevices = new File(sysBlock).listFiles();
        if (blockDevices == null) {
            return result;
        }
        for (File blockDev : blockDevices) {
            String name = blockDev.getName();
            if (name.startsWith("loop") || name.startsWith("ram")) {
                continue;
            }
            String sysPath = sysBlock + name + "/";
            String devnode = DevPath.NODE_ROOT + toDevName(name);
            long size = ParseUtil.parseLongOrDefault(FileUtil.getStringFromFile(sysPath + SIZE).trim(), 0L)
                    * SECTORSIZE;

            int[] diskMajMin = readMajorMinor(sysPath);
            // The udev database holds the properties libudev would return, when udevd runs on this host
            Map<String, String> udev = UdevData.queryBlockDevice(udevDataDir, diskMajMin[0], diskMajMin[1]);
            LinuxHWDiskStoreNF store;
            if (new File(sysPath + "dm").isDirectory()) {
                String dmUuid = FileUtil.getStringFromFile(sysPath + "dm/uuid").trim();
                store = new LinuxHWDiskStoreNF(devnode, getModelForDmDevice(dmUuid),
                        ParseUtil.getStringValueOrUnknown(dmUuid), size, "Virtual");
                String dmName = FileUtil.getStringFromFile(sysPath + "dm/name").trim();
                String[] vgLv = isLogicalVolume(dmUuid) ? splitLvmDmName(dmName) : new String[] { "", "" };
                addDeviceMapperPartition(store, mountsMap, dmUuid, vgLv[0], vgLv[1], dmName, devnode, name, sysPath,
                        udev.get(ID_FS_TYPE), udev.get(ID_FS_UUID), udev.get(ID_FS_LABEL), size, diskMajMin[0],
                        diskMajMin[1]);
            } else {
                // Prefer udev's values, as the libudev backends do; sysfs carries a serial only for some transports
                String model = ParseUtil.getStringValueOrDefault(udev.get(ID_MODEL),
                        FileUtil.getStringFromFile(sysPath + "device/model").trim());
                String serial = ParseUtil.getStringValueOrDefault(udev.get(ID_SERIAL_SHORT),
                        FileUtil.getStringFromFile(sysPath + "device/serial").trim());
                store = new LinuxHWDiskStoreNF(devnode, ParseUtil.getStringValueOrUnknown(model),
                        ParseUtil.getStringValueOrUnknown(serial), size, detectDiskType(sysPath));
            }

            // Read stats
            String statStr = FileUtil.getStringFromFile(sysPath + STAT).trim();
            if (!statStr.isEmpty()) {
                computeDiskStats(store, statStr);
            }

            // Enumerate partitions
            File[] partDirs = blockDev.listFiles(f -> f.isDirectory() && f.getName().startsWith(name));
            if (partDirs != null) {
                for (File partDir : partDirs) {
                    String partName = partDir.getName();
                    String partPath = sysPath + partName + "/";
                    long partSize = ParseUtil.parseLongOrDefault(FileUtil.getStringFromFile(partPath + SIZE).trim(), 0L)
                            * SECTORSIZE;
                    int[] majMin = readMajorMinor(partPath);
                    String partDevnode = DevPath.NODE_ROOT + toDevName(partName);
                    String mountPoint = mountsMap.getOrDefault(partDevnode,
                            getDependentNamesFromHoldersDirectory(partPath));
                    Map<String, String> partUdev = UdevData.queryBlockDevice(udevDataDir, majMin[0], majMin[1]);
                    store.getMutablePartitionList()
                            .add(new HWPartition(partDevnode, partName,
                                    ParseUtil.getStringValueOrDefault(partUdev.get(ID_FS_TYPE), PARTITION),
                                    ParseUtil.getStringValueOrEmpty(partUdev.get(ID_FS_UUID)),
                                    ParseUtil.getStringValueOrEmpty(partUdev.get(ID_FS_LABEL)), partSize, majMin[0],
                                    majMin[1], mountPoint));
                }
            }

            result.add(store);
        }
        finalizePartitions(result);
        return result;
    }

    /**
     * Reads a block device's major and minor numbers from its sysfs {@code dev} file.
     *
     * @param sysPath the device's sysfs path, ending in a slash
     * @return the major and minor numbers, each 0 if it could not be read
     */
    private static int[] readMajorMinor(String sysPath) {
        String devStr = FileUtil.getStringFromFile(sysPath + "dev").trim();
        int[] majMin = { 0, 0 };
        String[] split = devStr.split(":", -1);
        if (split.length == 2) {
            majMin[0] = ParseUtil.parseIntOrDefault(split[0], 0);
            majMin[1] = ParseUtil.parseIntOrDefault(split[1], 0);
        }
        return majMin;
    }

    /**
     * Detects the disk type from sysfs attributes, mirroring the udev-based backends' {@code detectDiskType}.
     *
     * @param sysPath the {@code /sys/block/<device>/} path for the device
     * @return {@code "Removable"}, {@code "SSD"}, {@code "HDD"}, or {@code "Unknown"}
     */
    private static String detectDiskType(String sysPath) {
        String removable = FileUtil.getStringFromFile(sysPath + "removable").trim();
        if ("1".equals(removable)) {
            return "Removable";
        }
        String rotational = FileUtil.getStringFromFile(sysPath + "queue/rotational").trim();
        if ("0".equals(rotational)) {
            return "SSD";
        } else if ("1".equals(rotational)) {
            return "HDD";
        }
        return "Unknown";
    }

    @Override
    public synchronized boolean updateAttributes() {
        return updateAttributes(SysPath.BLOCK);
    }

    /**
     * Updates this disk's statistics from its {@code stat} file under the given sysfs block directory. Package-private
     * for testing.
     *
     * @param sysBlock the sysfs block directory, normally {@link SysPath#BLOCK}
     * @return true if the statistics were read, false if the disk's {@code stat} file is missing or empty
     */
    synchronized boolean updateAttributes(String sysBlock) {
        String sysfsName = toSysfsName(getName().substring(DevPath.NODE_ROOT.length()));
        String statStr = FileUtil.getStringFromFile(sysBlock + sysfsName + "/" + STAT).trim();
        if (statStr.isEmpty()) {
            return false;
        }
        computeDiskStats(this, statStr);
        return true;
    }
}
