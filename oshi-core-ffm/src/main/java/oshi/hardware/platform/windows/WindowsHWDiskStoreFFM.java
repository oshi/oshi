/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSFTStorage;
import oshi.driver.common.windows.wmi.Win32DiskDrive;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition.DriveToPartitionProperty;
import oshi.driver.common.windows.wmi.Win32DiskPartition;
import oshi.driver.common.windows.wmi.Win32DiskPartition.DiskPartitionProperty;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition.DiskToPartitionProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.driver.windows.perfmon.PhysicalDiskFFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.WinNTFFM;
import oshi.ffm.platform.windows.WindowsForeignFunctions;
import oshi.ffm.platform.windows.com.FfmComException;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.common.platform.windows.WindowsHWDiskStore;
import oshi.util.Memoizer;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Windows hard disk implementation using FFM.
 */
@ThreadSafe
public final class WindowsHWDiskStoreFFM extends WindowsHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsHWDiskStoreFFM.class);

    private WindowsHWDiskStoreFFM(String name, String model, String serial, long size, String diskType,
            Supplier<Map<String, List<String>>> backingStores) {
        super(name, model, serial, size, diskType, backingStores);
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
            DiskStats stats = populateDiskStats(null, PhysicalDiskFFM.queryDiskCounters());
            PartitionMaps maps = queryPartitionMaps(h);

            WmiResult<DiskDriveProperty> vals = Win32DiskDrive.queryDiskDrive(h);
            int count = vals.getResultCount();

            // Ask the storage driver directly first: it needs no privilege, no COM, and is available on
            // every supported Windows version. Only if it leaves a disk unclassified is the Storage
            // namespace queried, which is slower and requires Windows 8.
            String[] diskTypes = classifyDiskTypes(vals, WindowsHWDiskStoreFFM::queryStorageProperties);
            if (anyDiskTypeUnknown(diskTypes)) {
                int savedTimeout = h.getWmiTimeout();
                if (savedTimeout < 0) {
                    h.setWmiTimeout(MSFT_PHYSICAL_DISK_TIMEOUT_MS);
                }
                try {
                    fillUnknownDiskTypes(diskTypes, vals, MSFTStorage.queryPhysicalDiskMediaTypes(h));
                } finally {
                    h.setWmiTimeout(savedTimeout);
                }
            }

            // Shared by the disks of this enumeration, and only queried when one is asked for its backing stores
            Supplier<Map<String, List<String>>> backingStores = Memoizer
                    .memoize(WindowsHWDiskStoreFFM::queryBackingStores);
            List<HWDiskStore> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                WindowsHWDiskStoreFFM ds = new WindowsHWDiskStoreFFM(WmiUtil.getString(vals, DiskDriveProperty.NAME, i),
                        (WmiUtil.getString(vals, DiskDriveProperty.MODEL, i) + " "
                                + WmiUtil.getString(vals, DiskDriveProperty.MANUFACTURER, i)).trim(),
                        ParseUtil.hexStringToString(WmiUtil.getString(vals, DiskDriveProperty.SERIALNUMBER, i)),
                        WmiUtil.getUint64(vals, DiskDriveProperty.SIZE, i), diskTypes[i], backingStores);

                String index = Integer.toString(WmiUtil.getUint32(vals, DiskDriveProperty.INDEX, i));
                ds.setDiskStats(stats, index);
                ds.setPartitionList(buildPartitionList(maps, ds.getName()));
                result.add(ds);
            }
            return Collections.unmodifiableList(result);
        } catch (FfmComException e) {
            LOG.warn("COM exception", e);
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }

    /**
     * Queries what each disk is built on, in its own COM session since it may run on any thread.
     *
     * @return the backing stores of each disk the Storage namespace reports, keyed by disk name, or an empty map if the
     *         namespace could not be queried
     */
    private static Map<String, List<String>> queryBackingStores() {
        WmiQueryHandlerFFM h = Objects.requireNonNull(WmiQueryHandlerFFM.createInstance());
        boolean comInit = false;
        int savedTimeout = h.getWmiTimeout();
        try {
            comInit = h.initCOM();
            if (savedTimeout < 0) {
                h.setWmiTimeout(MSFT_PHYSICAL_DISK_TIMEOUT_MS);
            }
            return queryBackingStores(h, WindowsHWDiskStoreFFM::queryVolumePathName);
        } catch (FfmComException e) {
            LOG.debug("Unable to query the Storage namespace for backing stores", e);
            return Collections.emptyMap();
        } finally {
            h.setWmiTimeout(savedTimeout);
            if (comInit) {
                h.unInitCOM();
            }
        }
    }

    private static final int MAX_PATH = 260;

    private static String queryVolumePathName(String path) {
        try (Arena arena = Arena.ofConfined()) {
            int len = MAX_PATH + 1;
            MemorySegment buf = arena.allocate(JAVA_CHAR, len);
            OptionalInt ok = Kernel32FFM.GetVolumePathName(WindowsForeignFunctions.toWideString(arena, path), buf, len);
            return ok.isPresent() && ok.getAsInt() != 0 ? WindowsForeignFunctions.readWideString(buf) : "";
        }
    }

    /**
     * Reads a physical drive's storage properties with {@code IOCTL_STORAGE_QUERY_PROPERTY}.
     *
     * @param name the device path from {@code Win32_DiskDrive}'s Name
     * @return the storage properties, or {@code null} if the device could not be opened or did not answer
     */
    private static @Nullable StorageProperties queryStorageProperties(String name) {
        if (!name.startsWith(PHYSICALDRIVE_PREFIX)) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            // Zero desired access is enough for IOCTL_STORAGE_QUERY_PROPERTY and needs no privilege
            Optional<MemorySegment> handle = Kernel32FFM.CreateFile(WindowsForeignFunctions.toWideString(arena, name),
                    0, WinNTFFM.FILE_SHARE_READ | WinNTFFM.FILE_SHARE_WRITE | WinNTFFM.FILE_SHARE_DELETE,
                    WinNTFFM.OPEN_EXISTING, 0);
            if (!handle.isPresent()) {
                LOG.debug("Couldn't open {} to query storage properties.", name);
                return null;
            }
            try {
                MemorySegment query = arena.allocate(STORAGE_PROPERTY_QUERY_SIZE);
                query.set(JAVA_INT, 0, STORAGE_DEVICE_PROPERTY);
                query.set(JAVA_INT, Integer.BYTES, PROPERTY_STANDARD_QUERY);
                MemorySegment deviceDescriptor = arena.allocate(STORAGE_DEVICE_DESCRIPTOR_BUFSIZE);
                if (!Kernel32FFM.DeviceIoControl(handle.get(), IOCTL_STORAGE_QUERY_PROPERTY, query,
                        STORAGE_PROPERTY_QUERY_SIZE, deviceDescriptor, STORAGE_DEVICE_DESCRIPTOR_BUFSIZE)) {
                    LOG.debug("Couldn't read the device descriptor for {}.", name);
                    return null;
                }
                boolean removableMedia = deviceDescriptor.get(JAVA_BYTE, SDD_REMOVABLE_MEDIA_OFFSET) != 0;
                int busType = deviceDescriptor.get(JAVA_INT, SDD_BUS_TYPE_OFFSET);

                // Not every storage driver implements the seek penalty property. A failure here is left as an
                // unknown rather than a guess, so the Storage namespace fallback can answer instead.
                Boolean incursSeekPenalty = null;
                query.set(JAVA_INT, 0, STORAGE_DEVICE_SEEK_PENALTY_PROPERTY);
                MemorySegment seekPenaltyDescriptor = arena.allocate(SEEK_PENALTY_DESCRIPTOR_SIZE);
                if (Kernel32FFM.DeviceIoControl(handle.get(), IOCTL_STORAGE_QUERY_PROPERTY, query,
                        STORAGE_PROPERTY_QUERY_SIZE, seekPenaltyDescriptor, SEEK_PENALTY_DESCRIPTOR_SIZE)) {
                    incursSeekPenalty = seekPenaltyDescriptor.get(JAVA_BYTE, SPD_INCURS_SEEK_PENALTY_OFFSET) != 0;
                }
                return new StorageProperties(busType, removableMedia, incursSeekPenalty);
            } finally {
                Kernel32FFM.CloseHandle(handle.get());
            }
        }
    }

    private static PartitionMaps queryPartitionMaps(WmiQueryHandlerFFM h) {
        PartitionMaps maps = new PartitionMaps();

        // Map drives to partitions
        WmiResult<DriveToPartitionProperty> drivePartitionMap = Win32DiskDriveToDiskPartition.queryDriveToPartition(h);
        for (int i = 0; i < drivePartitionMap.getResultCount(); i++) {
            mapDriveToPartition(maps, WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.ANTECEDENT, i),
                    WmiUtil.getRefString(drivePartitionMap, DriveToPartitionProperty.DEPENDENT, i));
        }

        // Map partitions to logical disks
        WmiResult<DiskToPartitionProperty> diskPartitionMap = Win32LogicalDiskToPartition.queryDiskToPartition(h);
        for (int i = 0; i < diskPartitionMap.getResultCount(); i++) {
            long size = WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.ENDINGADDRESS, i)
                    - WmiUtil.getUint64(diskPartitionMap, DiskToPartitionProperty.STARTINGADDRESS, i) + 1L;
            mapPartitionToLogicalDrive(maps,
                    WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.ANTECEDENT, i),
                    WmiUtil.getRefString(diskPartitionMap, DiskToPartitionProperty.DEPENDENT, i), size);
        }

        // Get all partitions and create objects
        WmiResult<DiskPartitionProperty> hwPartitionQueryMap = Win32DiskPartition.queryPartition(h);
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
