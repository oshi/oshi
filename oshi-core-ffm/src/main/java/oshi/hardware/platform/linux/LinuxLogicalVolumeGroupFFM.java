/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static org.slf4j.event.Level.WARN;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.io.File;
import java.lang.foreign.MemorySegment;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.NativeHandle;
import oshi.ffm.platform.linux.UdevFunctions;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.platform.linux.LinuxLogicalVolumeGroup;
import oshi.util.Util;
import oshi.util.linux.DevPath;

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
        return callInArenaOrDefault(arena -> {
            Map<String, Map<String, Set<String>>> logicalVolumesMap = new HashMap<>();
            Map<String, Set<String>> physicalVolumesMap = queryPhysicalVolumes();
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return Collections.emptyList();
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
                            String devnode = UdevFunctions.getString(UdevFunctions.udev_device_get_devnode(device),
                                    arena);
                            if (devnode != null && devnode.startsWith(DevPath.DM)) {
                                String uuid = UdevFunctions.getPropertyValue(device, DM_UUID, arena);
                                if (uuid != null && uuid.startsWith("LVM-")) {
                                    String vgName = UdevFunctions.getPropertyValue(device, DM_VG_NAME, arena);
                                    String lvName = UdevFunctions.getPropertyValue(device, DM_LV_NAME, arena);
                                    if (!Util.isBlank(vgName) && !Util.isBlank(lvName)) {
                                        logicalVolumesMap.computeIfAbsent(vgName, k -> new HashMap<>());
                                        Map<String, Set<String>> lvMapForGroup = logicalVolumesMap.get(vgName);
                                        physicalVolumesMap.computeIfAbsent(vgName, k -> new HashSet<>());
                                        Set<String> pvSetForGroup = physicalVolumesMap.get(vgName);
                                        File slavesDir = new File(syspath + "/slaves");
                                        File[] slaves = slavesDir.listFiles();
                                        if (slaves != null) {
                                            for (File f : slaves) {
                                                String pvName = f.getName();
                                                lvMapForGroup.computeIfAbsent(lvName, k -> new HashSet<>())
                                                        .add(DevPath.DEV + pvName);
                                                pvSetForGroup.add(DevPath.DEV + pvName);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return logicalVolumesMap.entrySet().stream()
                    .map(e -> new LinuxLogicalVolumeGroupFFM(e.getKey(), e.getValue(),
                            physicalVolumesMap.getOrDefault(e.getKey(), Collections.emptySet())))
                    .collect(Collectors.toList());
        }, LOG, WARN, "Error enumerating logical volume groups", Collections.emptyList());
    }
}
