/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.UdevFunctions;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.DevPath;

/**
 * FFM-based Linux hard disk implementation.
 */
@ThreadSafe
public final class LinuxHWDiskStoreFFM extends LinuxHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxHWDiskStoreFFM.class);

    LinuxHWDiskStoreFFM(String name, String model, String serial, long size) {
        super(name, model, serial, size);
    }

    /**
     * Gets the disks on this machine
     *
     * @return a list of {@link HWDiskStore} objects representing the disks
     */
    public static List<HWDiskStore> getDisks() {
        return getDisks(null);
    }

    static List<HWDiskStore> getDisks(LinuxHWDiskStore storeToUpdate) {
        if (!HAS_UDEV) {
            if (storeToUpdate == null) {
                LOG.warn("Disk Store information requires libudev, which is not present.");
            }
            return Collections.emptyList();
        }
        LinuxHWDiskStoreFFM store = null;
        List<HWDiskStore> result = new ArrayList<>();
        Map<String, String> mountsMap = readMountsMap();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                if (storeToUpdate == null) {
                    LOG.warn("Failed to create udev context for disk store enumeration.");
                }
                return Collections.emptyList();
            }
            try {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                try {
                    UdevFunctions.addMatchSubsystem(enumerate, BLOCK, arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);
                    for (MemorySegment entry = UdevFunctions
                            .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                    .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
                        if (syspath == null) {
                            continue;
                        }
                        MemorySegment device = UdevFunctions.deviceNewFromSyspath(udev, syspath, arena);
                        if (MemorySegment.NULL.equals(device)) {
                            continue;
                        }
                        try {
                            String devnode = UdevFunctions.getString(UdevFunctions.udev_device_get_devnode(device),
                                    arena);
                            if (devnode != null && !devnode.startsWith(DevPath.LOOP)
                                    && !devnode.startsWith(DevPath.RAM)) {
                                String devtype = UdevFunctions.getString(UdevFunctions.udev_device_get_devtype(device),
                                        arena);
                                if (DISK.equals(devtype)) {
                                    String devModel = UdevFunctions.getPropertyValue(device, ID_MODEL, arena);
                                    String devSerial = UdevFunctions.getPropertyValue(device, ID_SERIAL_SHORT, arena);
                                    long devSize = ParseUtil.parseLongOrDefault(
                                            UdevFunctions.getSysattrValue(device, SIZE, arena), 0L) * SECTORSIZE;
                                    if (devnode.startsWith(DevPath.DM)) {
                                        devModel = LOGICAL_VOLUME_GROUP;
                                        devSerial = UdevFunctions.getPropertyValue(device, DM_UUID, arena);
                                        store = new LinuxHWDiskStoreFFM(devnode, devModel,
                                                devSerial == null ? Constants.UNKNOWN : devSerial, devSize);
                                        if (devSerial != null && devSerial.startsWith("LVM-")) {
                                            String vgName = UdevFunctions.getPropertyValue(device, DM_VG_NAME, arena);
                                            String lvName = UdevFunctions.getPropertyValue(device, DM_LV_NAME, arena);
                                            String fsType = UdevFunctions.getPropertyValue(device, ID_FS_TYPE, arena);
                                            String fsUuid = UdevFunctions.getPropertyValue(device, ID_FS_UUID, arena);
                                            String fsLabel = UdevFunctions.getPropertyValue(device, ID_FS_LABEL, arena);
                                            String sysname = UdevFunctions
                                                    .getString(UdevFunctions.udev_device_get_sysname(device), arena);
                                            store.getMutablePartitionList().add(new HWPartition(
                                                    getPartitionNameForDmDevice(vgName, lvName), sysname,
                                                    fsType == null ? PARTITION : fsType, fsUuid == null ? "" : fsUuid,
                                                    fsLabel == null ? "" : fsLabel,
                                                    ParseUtil.parseLongOrDefault(
                                                            UdevFunctions.getSysattrValue(device, SIZE, arena), 0L)
                                                            * SECTORSIZE,
                                                    ParseUtil.parseIntOrDefault(
                                                            UdevFunctions.getPropertyValue(device, MAJOR, arena), 0),
                                                    ParseUtil.parseIntOrDefault(
                                                            UdevFunctions.getPropertyValue(device, MINOR, arena), 0),
                                                    getMountPointOfDmDevice(vgName, lvName)));
                                        }
                                    } else {
                                        store = new LinuxHWDiskStoreFFM(devnode,
                                                devModel == null ? Constants.UNKNOWN : devModel,
                                                devSerial == null ? Constants.UNKNOWN : devSerial, devSize);
                                    }
                                    if (storeToUpdate == null) {
                                        computeDiskStats(store, UdevFunctions.getSysattrValue(device, STAT, arena));
                                        result.add(store);
                                    } else if (store.getName().equals(storeToUpdate.getName())
                                            && store.getModel().equals(storeToUpdate.getModel())
                                            && store.getSerial().equals(storeToUpdate.getSerial())
                                            && store.getSize() == storeToUpdate.getSize()) {
                                        computeDiskStats(storeToUpdate,
                                                UdevFunctions.getSysattrValue(device, STAT, arena));
                                        result.add(storeToUpdate);
                                        break;
                                    }
                                } else if (storeToUpdate == null && store != null && PARTITION.equals(devtype)) {
                                    MemorySegment parent = UdevFunctions.getParentWithSubsystemDevtype(device, BLOCK,
                                            DISK, arena);
                                    if (!MemorySegment.NULL.equals(parent)) {
                                        String parentDevnode = UdevFunctions
                                                .getString(UdevFunctions.udev_device_get_devnode(parent), arena);
                                        if (store.getName().equals(parentDevnode)) {
                                            String name = devnode;
                                            String sysname = UdevFunctions
                                                    .getString(UdevFunctions.udev_device_get_sysname(device), arena);
                                            String sysPath = UdevFunctions
                                                    .getString(UdevFunctions.udev_device_get_syspath(device), arena);
                                            String fsType = UdevFunctions.getPropertyValue(device, ID_FS_TYPE, arena);
                                            String fsUuid = UdevFunctions.getPropertyValue(device, ID_FS_UUID, arena);
                                            String fsLabel = UdevFunctions.getPropertyValue(device, ID_FS_LABEL, arena);
                                            store.getMutablePartitionList().add(new HWPartition(name, sysname,
                                                    fsType == null ? PARTITION : fsType, fsUuid == null ? "" : fsUuid,
                                                    fsLabel == null ? "" : fsLabel,
                                                    ParseUtil.parseLongOrDefault(
                                                            UdevFunctions.getSysattrValue(device, SIZE, arena), 0L)
                                                            * SECTORSIZE,
                                                    ParseUtil.parseIntOrDefault(
                                                            UdevFunctions.getPropertyValue(device, MAJOR, arena), 0),
                                                    ParseUtil.parseIntOrDefault(
                                                            UdevFunctions.getPropertyValue(device, MINOR, arena), 0),
                                                    mountsMap.getOrDefault(name,
                                                            getDependentNamesFromHoldersDirectory(sysPath))));
                                        }
                                    }
                                }
                            }
                        } finally {
                            UdevFunctions.udev_device_unref(device);
                        }
                    }
                } finally {
                    UdevFunctions.udev_enumerate_unref(enumerate);
                }
            } finally {
                UdevFunctions.udev_unref(udev);
            }
        } catch (Throwable e) {
            if (storeToUpdate == null) {
                LOG.warn("Error enumerating disk stores: {}", e.getMessage());
            }
            return Collections.emptyList();
        }
        finalizePartitions(result);
        return result;
    }

    @Override
    public boolean updateAttributes() {
        return !getDisks(this).isEmpty();
    }
}
