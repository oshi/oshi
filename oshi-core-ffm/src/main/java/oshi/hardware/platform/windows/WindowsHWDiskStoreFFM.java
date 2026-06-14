/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static java.lang.foreign.ValueLayout.JAVA_CHAR;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition.DriveToPartitionProperty;
import oshi.driver.common.windows.wmi.Win32DiskPartition.DiskPartitionProperty;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition.DiskToPartitionProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.perfmon.PhysicalDiskFFM;
import oshi.driver.windows.wmi.Win32DiskDriveFFM;
import oshi.driver.windows.wmi.Win32DiskDriveToDiskPartitionFFM;
import oshi.driver.windows.wmi.Win32DiskPartitionFFM;
import oshi.driver.windows.wmi.Win32LogicalDiskToPartitionFFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.WindowsForeignFunctions;
import oshi.ffm.platform.windows.com.FfmComException;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.windows.WindowsHWDiskStore;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Windows hard disk implementation using FFM.
 */
@ThreadSafe
public final class WindowsHWDiskStoreFFM extends WindowsHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStoreFFM.class);

    private WindowsHWDiskStoreFFM(String name, String model, String serial, long size, String diskType) {
        super(name, model, serial, size, diskType);
    }

    @Override
    protected DiskStats queryReadWriteStats(String index) {
        return populateDiskStats(index, PhysicalDiskFFM.queryDiskCounters());
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        WmiQueryHandlerFFM h = Objects.requireNonNull(WmiQueryHandlerFFM.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            List<HWDiskStore> result = new ArrayList<>();
            DiskStats stats = populateDiskStats(null, PhysicalDiskFFM.queryDiskCounters());
            PartitionMaps maps = queryPartitionMaps(h);

            WmiResult<DiskDriveProperty> vals = Win32DiskDriveFFM.queryDiskDrive(h);
            for (int i = 0; i < vals.getResultCount(); i++) {
                WindowsHWDiskStoreFFM ds = new WindowsHWDiskStoreFFM(WmiUtil.getString(vals, DiskDriveProperty.NAME, i),
                        String.format(Locale.ROOT, "%s %s", WmiUtil.getString(vals, DiskDriveProperty.MODEL, i),
                                WmiUtil.getString(vals, DiskDriveProperty.MANUFACTURER, i)).trim(),
                        ParseUtil.hexStringToString(WmiUtil.getString(vals, DiskDriveProperty.SERIALNUMBER, i)),
                        WmiUtil.getUint64(vals, DiskDriveProperty.SIZE, i),
                        parseWindowsMediaType(WmiUtil.getString(vals, DiskDriveProperty.MEDIATYPE, i)));

                String index = Integer.toString(WmiUtil.getUint32(vals, DiskDriveProperty.INDEX, i));
                ds.setDiskStats(stats, index);
                ds.setPartitionList(buildPartitionList(maps, ds.getName()));
                result.add(ds);
            }
            return result;
        } catch (FfmComException e) {
            LOG.warn("COM exception: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }

    private static PartitionMaps queryPartitionMaps(WmiQueryHandlerFFM h) {
        PartitionMaps maps = new PartitionMaps();

        // Map drives to partitions
        WmiResult<DriveToPartitionProperty> drivePartitionMap = Win32DiskDriveToDiskPartitionFFM
                .queryDriveToPartition(h);
        for (int i = 0; i < drivePartitionMap.getResultCount(); i++) {
            mapDriveToPartition(maps, WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.ANTECEDENT, i),
                    WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.DEPENDENT, i));
        }

        // Map partitions to logical disks
        WmiResult<DiskToPartitionProperty> diskPartitionMap = Win32LogicalDiskToPartitionFFM.queryDiskToPartition(h);
        for (int i = 0; i < diskPartitionMap.getResultCount(); i++) {
            long size = WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.ENDINGADDRESS, i)
                    - WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.STARTINGADDRESS, i) + 1L;
            mapPartitionToLogicalDrive(maps,
                    WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.ANTECEDENT, i),
                    WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.DEPENDENT, i), size);
        }

        // Get all partitions and create objects
        WmiResult<DiskPartitionProperty> hwPartitionQueryMap = Win32DiskPartitionFFM.queryPartition(h);
        for (int i = 0; i < hwPartitionQueryMap.getResultCount(); i++) {
            String deviceID = WmiUtil.getString(hwPartitionQueryMap, DiskPartitionProperty.DEVICEID, i);
            List<Pair<String, Long>> logicalDrives = maps.getPartitionToLogicalDriveMap().get(deviceID);
            if (logicalDrives == null) {
                continue;
            }
            for (int j = 0; j < logicalDrives.size(); j++) {
                Pair<String, Long> logicalDrive = logicalDrives.get(j);
                if (logicalDrive != null && !logicalDrive.getA().isEmpty()) {
                    String uuid;
                    String label = "";
                    try (Arena arena = Arena.ofConfined()) {
                        MemorySegment mountPoint = WindowsForeignFunctions.toWideString(arena, logicalDrive.getA());
                        MemorySegment volumeBuf = arena.allocate(JAVA_CHAR, GUID_BUFSIZE);
                        Kernel32FFM.GetVolumeNameForVolumeMountPoint(mountPoint, volumeBuf, GUID_BUFSIZE);
                        uuid = ParseUtil.parseUuidOrDefault(WindowsForeignFunctions.readWideString(volumeBuf), "");

                        MemorySegment labelBuf = arena.allocate(JAVA_CHAR, LABEL_BUFSIZE);
                        OptionalInt volInfoResult = Kernel32FFM.GetVolumeInformation(mountPoint, labelBuf,
                                LABEL_BUFSIZE, MemorySegment.NULL, MemorySegment.NULL, MemorySegment.NULL,
                                MemorySegment.NULL, 0);
                        if (volInfoResult.isPresent() && volInfoResult.getAsInt() != 0) {
                            label = WindowsForeignFunctions.readWideString(labelBuf);
                        } else {
                            OptionalInt error = Kernel32FFM.GetLastError();
                            LOG.debug("Failed to get volume label for {}: error code {}", logicalDrive.getA(),
                                    error.orElse(-1));
                        }
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
