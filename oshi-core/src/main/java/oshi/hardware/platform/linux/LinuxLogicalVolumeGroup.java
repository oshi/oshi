/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.DevPath;

class LinuxLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    protected static final String BLOCK = "block";
    protected static final String DM_UUID = "DM_UUID";
    protected static final String DM_VG_NAME = "DM_VG_NAME";
    protected static final String DM_LV_NAME = "DM_LV_NAME";

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
        Map<String, Set<String>> physicalVolumesMap = new HashMap<>();
        for (String s : ExecutingCommand.runNative("pvs -o vg_name,pv_name")) {
            String[] split = ParseUtil.whitespaces.split(s.trim());
            if (split.length == 2 && split[1].startsWith(DevPath.DEV)) {
                physicalVolumesMap.computeIfAbsent(split[0], k -> new HashSet<>()).add(split[1]);
            }
        }
        return physicalVolumesMap;
    }
}
