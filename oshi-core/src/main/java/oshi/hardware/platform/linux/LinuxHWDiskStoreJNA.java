/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystem.HAS_UDEV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.DevPath;

/**
 * JNA-based Linux hard disk implementation.
 */
@ThreadSafe
public final class LinuxHWDiskStoreJNA extends LinuxHWDiskStore {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxHWDiskStoreJNA.class);

    LinuxHWDiskStoreJNA(String name, String model, String serial, long size) {
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
            LOG.warn("Disk Store information requires libudev, which is not present.");
            return Collections.emptyList();
        }
        LinuxHWDiskStoreJNA store = null;
        List<HWDiskStore> result = new ArrayList<>();
        Map<String, String> mountsMap = readMountsMap();

        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return Collections.emptyList();
        }
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem(BLOCK);
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            String devnode = device.getDevnode();
                            if (devnode != null && !devnode.startsWith(DevPath.LOOP)
                                    && !devnode.startsWith(DevPath.RAM)) {
                                if (DISK.equals(device.getDevtype())) {
                                    String devModel = device.getPropertyValue(ID_MODEL);
                                    String devSerial = device.getPropertyValue(ID_SERIAL_SHORT);
                                    long devSize = ParseUtil.parseLongOrDefault(device.getSysattrValue(SIZE), 0L)
                                            * SECTORSIZE;
                                    if (devnode.startsWith(DevPath.DM)) {
                                        devModel = LOGICAL_VOLUME_GROUP;
                                        devSerial = device.getPropertyValue(DM_UUID);
                                        store = new LinuxHWDiskStoreJNA(devnode, devModel,
                                                devSerial == null ? Constants.UNKNOWN : devSerial, devSize);
                                        String vgName = device.getPropertyValue(DM_VG_NAME);
                                        String lvName = device.getPropertyValue(DM_LV_NAME);
                                        if (vgName != null && lvName != null && devSerial != null
                                                && devSerial.startsWith("LVM-")) {
                                            store.getMutablePartitionList().add(new HWPartition(
                                                    getPartitionNameForDmDevice(vgName, lvName), device.getSysname(),
                                                    device.getPropertyValue(ID_FS_TYPE) == null ? PARTITION
                                                            : device.getPropertyValue(ID_FS_TYPE),
                                                    device.getPropertyValue(ID_FS_UUID) == null ? ""
                                                            : device.getPropertyValue(ID_FS_UUID),
                                                    device.getPropertyValue(ID_FS_LABEL) == null ? ""
                                                            : device.getPropertyValue(ID_FS_LABEL),
                                                    ParseUtil.parseLongOrDefault(device.getSysattrValue(SIZE), 0L)
                                                            * SECTORSIZE,
                                                    ParseUtil.parseIntOrDefault(device.getPropertyValue(MAJOR), 0),
                                                    ParseUtil.parseIntOrDefault(device.getPropertyValue(MINOR), 0),
                                                    getMountPointOfDmDevice(vgName, lvName)));
                                        }
                                    } else {
                                        store = new LinuxHWDiskStoreJNA(devnode,
                                                devModel == null ? Constants.UNKNOWN : devModel,
                                                devSerial == null ? Constants.UNKNOWN : devSerial, devSize);
                                    }
                                    if (storeToUpdate == null) {
                                        computeDiskStats(store, device.getSysattrValue(STAT));
                                        result.add(store);
                                    } else if (store.getName().equals(storeToUpdate.getName())
                                            && store.getModel().equals(storeToUpdate.getModel())
                                            && store.getSerial().equals(storeToUpdate.getSerial())
                                            && store.getSize() == storeToUpdate.getSize()) {
                                        computeDiskStats(storeToUpdate, device.getSysattrValue(STAT));
                                        result.add(storeToUpdate);
                                        break;
                                    }
                                } else if (storeToUpdate == null && store != null
                                        && PARTITION.equals(device.getDevtype())) {
                                    UdevDevice parent = device.getParentWithSubsystemDevtype(BLOCK, DISK);
                                    if (parent != null && store.getName().equals(parent.getDevnode())) {
                                        String name = device.getDevnode();
                                        store.getMutablePartitionList().add(new HWPartition(name, device.getSysname(),
                                                device.getPropertyValue(ID_FS_TYPE) == null ? PARTITION
                                                        : device.getPropertyValue(ID_FS_TYPE),
                                                device.getPropertyValue(ID_FS_UUID) == null ? ""
                                                        : device.getPropertyValue(ID_FS_UUID),
                                                device.getPropertyValue(ID_FS_LABEL) == null ? ""
                                                        : device.getPropertyValue(ID_FS_LABEL),
                                                ParseUtil.parseLongOrDefault(device.getSysattrValue(SIZE), 0L)
                                                        * SECTORSIZE,
                                                ParseUtil.parseIntOrDefault(device.getPropertyValue(MAJOR), 0),
                                                ParseUtil.parseIntOrDefault(device.getPropertyValue(MINOR), 0),
                                                mountsMap.getOrDefault(name,
                                                        getDependentNamesFromHoldersDirectory(device.getSyspath()))));
                                    }
                                }
                            }
                        } finally {
                            device.unref();
                        }
                    }
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        finalizePartitions(result);
        return result;
    }

    @Override
    public boolean updateAttributes() {
        return !getDisks(this).isEmpty();
    }
}
