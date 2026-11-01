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
        return getDisks(SysPath.BLOCK, readMountsMap());
    }

    /**
     * Gets the disk stores under a sysfs block directory, listing the same devices as the udev backends: every block
     * device except loop and RAM disks. Package-private for testing.
     *
     * @param sysBlock  the sysfs block directory, normally {@link SysPath#BLOCK}
     * @param mountsMap the map of device paths to mount points
     * @return a list of {@link HWDiskStore} objects
     */
    static List<HWDiskStore> getDisks(String sysBlock, Map<String, String> mountsMap) {
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
            String devnode = DevPath.DEV + toDevName(name);
            long size = ParseUtil.parseLongOrDefault(FileUtil.getStringFromFile(sysPath + SIZE).trim(), 0L)
                    * SECTORSIZE;

            LinuxHWDiskStoreNF store;
            if (new File(sysPath + "dm").isDirectory()) {
                String dmUuid = FileUtil.getStringFromFile(sysPath + "dm/uuid").trim();
                store = new LinuxHWDiskStoreNF(devnode, getModelForDmDevice(dmUuid),
                        ParseUtil.getStringValueOrUnknown(dmUuid), size, "Virtual");
                String dmName = FileUtil.getStringFromFile(sysPath + "dm/name").trim();
                String[] vgLv = isLogicalVolume(dmUuid) ? splitLvmDmName(dmName) : new String[] { "", "" };
                int[] majMin = readMajorMinor(sysPath);
                addDeviceMapperPartition(store, mountsMap, dmUuid, vgLv[0], vgLv[1], dmName, devnode, name, sysPath,
                        null, null, null, size, majMin[0], majMin[1]);
            } else {
                store = new LinuxHWDiskStoreNF(devnode,
                        ParseUtil.getStringValueOrUnknown(FileUtil.getStringFromFile(sysPath + "device/model").trim()),
                        ParseUtil.getStringValueOrUnknown(FileUtil.getStringFromFile(sysPath + "device/serial").trim()),
                        size, detectDiskType(sysPath));
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
                    String partDevnode = DevPath.DEV + toDevName(partName);
                    String mountPoint = mountsMap.getOrDefault(partDevnode,
                            getDependentNamesFromHoldersDirectory(partPath));
                    store.getMutablePartitionList().add(new HWPartition(partDevnode, partName, "", "", "", partSize,
                            majMin[0], majMin[1], mountPoint));
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
        String sysfsName = toSysfsName(getName().substring(DevPath.DEV.length()));
        String statStr = FileUtil.getStringFromFile(sysBlock + sysfsName + "/" + STAT).trim();
        if (statStr.isEmpty()) {
            return false;
        }
        computeDiskStats(this, statStr);
        return true;
    }
}
