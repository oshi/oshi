/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.Kernel32;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition.DriveToPartitionProperty;
import oshi.driver.common.windows.wmi.Win32DiskPartition.DiskPartitionProperty;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition.DiskToPartitionProperty;
import oshi.driver.windows.perfmon.PhysicalDiskJNA;
import oshi.driver.windows.wmi.Win32DiskDriveJNA;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartitionJNA;
import oshi.driver.windows.wmi.Win32DiskPartitionJNA;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartitionJNA;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.windows.WindowsHWDiskStore;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

/**
 * Windows hard disk implementation using JNA.
 */
@ThreadSafe
public final class WindowsHWDiskStoreJNA extends WindowsHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStoreJNA.class);

    private WindowsHWDiskStoreJNA(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    @Override
    protected DiskStats queryReadWriteStats(String index) {
        return populateDiskStats(index, PhysicalDiskJNA.queryDiskCounters());
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            List<HWDiskStore> result = new ArrayList<>();
            DiskStats stats = populateDiskStats(null, PhysicalDiskJNA.queryDiskCounters());
            PartitionMaps maps = queryPartitionMaps(h);

            WmiResult<DiskDriveProperty> vals = Win32DiskDriveJNA.queryDiskDrive(h);
            for (int i = 0; i < vals.getResultCount(); i++) {
                WindowsHWDiskStoreJNA ds = new WindowsHWDiskStoreJNA(WmiUtil.getString(vals, DiskDriveProperty.NAME, i),
                        String.format(Locale.ROOT, "%s %s", WmiUtil.getString(vals, DiskDriveProperty.MODEL, i),
                                WmiUtil.getString(vals, DiskDriveProperty.MANUFACTURER, i)).trim(),
                        ParseUtil.hexStringToString(WmiUtil.getString(vals, DiskDriveProperty.SERIALNUMBER, i)),
                        WmiUtil.getUint64(vals, DiskDriveProperty.SIZE, i));

                String index = Integer.toString(WmiUtil.getUint32(vals, DiskDriveProperty.INDEX, i));
                ds.setDiskStats(stats, index);
                ds.setPartitionList(buildPartitionList(maps, ds.getName()));
                result.add(ds);
            }
            return result;
        } catch (COMException e) {
            LOG.warn("COM exception: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }

    private static PartitionMaps queryPartitionMaps(WmiQueryHandler h) {
        PartitionMaps maps = new PartitionMaps();

        // Map drives to partitions
        WmiResult<DriveToPartitionProperty> drivePartitionMap = Win32DiskDriveToDiskPartitionJNA
                .queryDriveToPartition(h);
        for (int i = 0; i < drivePartitionMap.getResultCount(); i++) {
            mapDriveToPartition(maps, WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.ANTECEDENT, i),
                    WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.DEPENDENT, i));
        }

        // Map partitions to logical disks
        WmiResult<DiskToPartitionProperty> diskPartitionMap = Win32LogicalDiskToPartitionJNA.queryDiskToPartition(h);
        for (int i = 0; i < diskPartitionMap.getResultCount(); i++) {
            long size = WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.ENDINGADDRESS, i)
                    - WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.STARTINGADDRESS, i) + 1L;
            mapPartitionToLogicalDrive(maps,
                    WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.ANTECEDENT, i),
                    WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.DEPENDENT, i), size);
        }

        // Get all partitions and create objects
        WmiResult<DiskPartitionProperty> hwPartitionQueryMap = Win32DiskPartitionJNA.queryPartition(h);
        for (int i = 0; i < hwPartitionQueryMap.getResultCount(); i++) {
            String deviceID = WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.DEVICEID, i);
            List<Pair<String, Long>> logicalDrives = maps.getPartitionToLogicalDriveMap().get(deviceID);
            if (logicalDrives == null) {
                continue;
            }
            for (int j = 0; j < logicalDrives.size(); j++) {
                Pair<String, Long> logicalDrive = logicalDrives.get(j);
                if (logicalDrive != null && !logicalDrive.getA().isEmpty()) {
                    char[] volumeChr = new char[GUID_BUFSIZE];
                    Kernel32.INSTANCE.GetVolumeNameForVolumeMountPoint(logicalDrive.getA(), volumeChr, GUID_BUFSIZE);
                    String uuid = ParseUtil.parseUuidOrDefault(new String(volumeChr).trim(), "");
                    char[] labelChr = new char[LABEL_BUFSIZE];
                    String label = "";
                    if (Kernel32.INSTANCE.GetVolumeInformation(logicalDrive.getA(), labelChr, LABEL_BUFSIZE, null, null,
                            null, null, 0)) {
                        label = new String(labelChr).trim();
                    } else {
                        int error = Kernel32.INSTANCE.GetLastError();
                        LOG.debug("Failed to get volume label for {}: error code {}", logicalDrive.getA(), error);
                    }
                    HWPartition pt = new HWPartition(
                            WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.NAME, i),
                            WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.TYPE, i),
                            WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.DESCRIPTION, i), uuid, label,
                            logicalDrive.getB(),
                            WmiUtil.getUint32(hwPartitionQueryMap, DiskPartitionProperty.DISKINDEX, i),
                            WmiUtil.getUint32(hwPartitionQueryMap, DiskPartitionProperty.INDEX, i),
                            logicalDrive.getA());
                    maps.getPartitionMap().computeIfAbsent(deviceID, x -> new ArrayList<>()).add(pt);
                }
            }
        }
        return maps;
    }
}
