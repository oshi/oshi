/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;

import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.platform.linux.LinuxLogicalVolumeGroup;

/**
 * JNA-based Linux logical volume group implementation.
 */
final class LinuxLogicalVolumeGroupJNA extends LinuxLogicalVolumeGroup {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxLogicalVolumeGroupJNA.class);

    LinuxLogicalVolumeGroupJNA(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        if (!HAS_UDEV) {
            LOG.warn("Logical Volume Group information requires libudev, which is not present.");
            return Collections.emptyList();
        }
        return buildLogicalVolumeGroups(enumerateBlockDevices(), LinuxLogicalVolumeGroupJNA::new);
    }

    private static List<UdevBlockDevice> enumerateBlockDevices() {
        List<UdevBlockDevice> devices = new ArrayList<>();
        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return devices;
        }
        try {
            Udev.UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem(BLOCK);
                enumerate.scanDevices();
                for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    if (syspath == null) {
                        continue;
                    }
                    Udev.UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            devices.add(
                                    new UdevBlockDevice(syspath, device.getDevnode(), device.getPropertyValue(DM_UUID),
                                            device.getPropertyValue(DM_VG_NAME), device.getPropertyValue(DM_LV_NAME)));
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
        return devices;
    }
}
