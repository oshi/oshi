/*
 * Copyright 2021-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import oshi.hardware.LogicalVolumeGroup;

public class AbstractLogicalVolumeGroup implements LogicalVolumeGroup {

    private final String name;
    private final Map<String, Set<String>> lvMap;
    private final Set<String> pvSet;

    /**
     * @param name  Name of the volume group
     * @param lvMap Logical volumes derived from this volume group and the physical volumes its mapped to.
     * @param pvSet Set of physical volumes this volume group consists of.
     */
    protected AbstractLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        this.name = name;
        for (Entry<String, Set<String>> entry : lvMap.entrySet()) {
            lvMap.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue()));
        }
        this.lvMap = Collections.unmodifiableMap(lvMap);
        this.pvSet = Collections.unmodifiableSet(pvSet);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Map<String, Set<String>> getLogicalVolumes() {
        return lvMap;
    }

    @Override
    public Set<String> getPhysicalVolumes() {
        return pvSet;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Logical Volume Group: ");
        sb.append(name).append("\n |-- PVs: ");
        sb.append(pvSet.toString());
        for (Entry<String, Set<String>> entry : lvMap.entrySet()) {
            sb.append("\n |-- LV: ").append(entry.getKey());
            Set<String> mappedPVs = entry.getValue();
            if (!mappedPVs.isEmpty()) {
                sb.append(" --> ").append(mappedPVs);
            }
        }
        return sb.toString();
    }
}
