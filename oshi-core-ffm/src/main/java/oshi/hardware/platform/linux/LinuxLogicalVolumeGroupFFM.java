/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.NativeHandle;
import oshi.ffm.platform.linux.UdevFunctions;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.platform.linux.LinuxLogicalVolumeGroup;

/**
 * FFM-based Linux logical volume group implementation.
 */
final class LinuxLogicalVolumeGroupFFM extends LinuxLogicalVolumeGroup {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxLogicalVolumeGroupFFM.class);

    LinuxLogicalVolumeGroupFFM(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        if (!HAS_UDEV) {
            LOG.warn("Logical Volume Group information requires libudev, which is not present.");
            return Collections.emptyList();
        }
        return buildLogicalVolumeGroups(enumerateBlockDevices(), LinuxLogicalVolumeGroupFFM::new);
    }

    private static List<UdevBlockDevice> enumerateBlockDevices() {
        return callInArenaOrDefault(arena -> {
            List<UdevBlockDevice> devices = new ArrayList<>();
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return devices;
            }
            // wrapped only to release the native handle on close
            try (var _ = NativeHandle.of(udev, UdevFunctions::udev_unref)) {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                // wrapped only to release the native handle on close
                try (var _ = NativeHandle.of(enumerate, UdevFunctions::udev_enumerate_unref)) {
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
                        // wrapped only to release the native handle on close
                        try (var _ = NativeHandle.of(device, UdevFunctions::udev_device_unref)) {
                            devices.add(
                                    new UdevBlockDevice(syspath,
                                            UdevFunctions.getString(UdevFunctions.udev_device_get_devnode(device),
                                                    arena),
                                            UdevFunctions.getPropertyValue(device, DM_UUID, arena),
                                            UdevFunctions.getPropertyValue(device, DM_VG_NAME, arena),
                                            UdevFunctions.getPropertyValue(device, DM_LV_NAME, arena)));
                        }
                    }
                }
            }
            return devices;
        }, LOG, WARN, "Error enumerating logical volume groups", Collections.emptyList());
    }
}
