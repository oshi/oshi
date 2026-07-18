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

    private static final String SYS_BLOCK = SysPath.SYS + "block/";

    LinuxHWDiskStoreNF(String name, String model, String serial, long size, String diskType) {
        super(name, model, serial, size, diskType);
    }

    /**
     * Gets disk stores on this machine.
     *
     * @return a list of {@link HWDiskStore} objects
     */
    public static List<HWDiskStore> getDisks() {
        List<HWDiskStore> result = new ArrayList<>();
        Map<String, String> mountsMap = readMountsMap();

        File[] blockDevices = new File(SYS_BLOCK).listFiles();
        if (blockDevices == null) {
            return result;
        }
        for (File blockDev : blockDevices) {
            String name = blockDev.getName();
            String sysPath = SYS_BLOCK + name + "/";

            // Filter to real disks (skip loop, ram, etc.)
            String devType = FileUtil.getStringFromFile(sysPath + "device/type").trim();
            // No device/type means it's a virtual device; check if it has a device/ subdir
            if (devType.isEmpty() && !new File(sysPath + "device").exists()) {
                continue;
            }

            String model = FileUtil.getStringFromFile(sysPath + "device/model").trim();
            String serial = FileUtil.getStringFromFile(sysPath + "device/serial").trim();
            long size = ParseUtil.parseLongOrDefault(FileUtil.getStringFromFile(sysPath + SIZE).trim(), 0L)
                    * SECTORSIZE;
            String diskType = detectDiskType(sysPath);

            LinuxHWDiskStoreNF store = new LinuxHWDiskStoreNF(DevPath.DEV + name, model, serial, size, diskType);

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
                    String devStr = FileUtil.getStringFromFile(partPath + "dev").trim();
                    int major = 0;
                    int minor = 0;
                    if (devStr.contains(":")) {
                        String[] majMin = devStr.split(":");
                        major = ParseUtil.parseIntOrDefault(majMin[0], 0);
                        minor = ParseUtil.parseIntOrDefault(majMin[1], 0);
                    }
                    String mountPoint = mountsMap.getOrDefault(DevPath.DEV + partName, "");
                    store.getMutablePartitionList()
                            .add(new HWPartition(partName, partName, "", "", "", partSize, major, minor, mountPoint));
                }
            }

            result.add(store);
        }
        finalizePartitions(result);
        return result;
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
    public boolean updateAttributes() {
        String devName = getName().replace(DevPath.DEV, "");
        String statStr = FileUtil.getStringFromFile(SYS_BLOCK + devName + "/" + STAT).trim();
        if (statStr.isEmpty()) {
            return false;
        }
        computeDiskStats(this, statStr);
        return true;
    }
}
