/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.linux.DevPath;

/**
 * Linux implementation of LogicalVolumeGroup.
 */
public class LinuxLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    /** Sysfs block device type. */
    protected static final String BLOCK = "block";
    /** Device-mapper UUID property. */
    protected static final String DM_UUID = "DM_UUID";
    /** Device-mapper volume group name property. */
    protected static final String DM_VG_NAME = "DM_VG_NAME";
    /** Device-mapper logical volume name property. */
    protected static final String DM_LV_NAME = "DM_LV_NAME";

    /**
     * Creates a LinuxLogicalVolumeGroup.
     *
     * @param name  the volume group name
     * @param lvMap the logical volume map
     * @param pvSet the physical volume set
     */
    protected LinuxLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    /**
     * Populates the physical volumes map from the {@code pvs} command output. Requires elevated permissions; may return
     * an empty map if the command fails.
     *
     * @return map of VG name to set of PV device paths
     */
    protected static Map<String, Set<String>> queryPhysicalVolumes() {
        return parsePhysicalVolumes(ExecutingCommand.runNative("pvs -o vg_name,pv_name"));
    }

    /**
     * Parses {@code pvs -o vg_name,pv_name} output into a map of volume-group name to physical-volume device paths.
     * Package-private for testing.
     *
     * @param pvs the lines of {@code pvs -o vg_name,pv_name} output
     * @return map of VG name to set of PV device paths
     */
    static Map<String, Set<String>> parsePhysicalVolumes(List<String> pvs) {
        Map<String, Set<String>> physicalVolumesMap = new HashMap<>();
        for (String s : pvs) {
            String[] split = ParseUtil.whitespaces.split(s.trim());
            if (split.length == 2 && split[1].startsWith(DevPath.DEV)) {
                physicalVolumesMap.computeIfAbsent(split[0], k -> new HashSet<>()).add(split[1]);
            }
        }
        return physicalVolumesMap;
    }
}
