/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.Util;
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
            String[] split = ParseUtil.whitespaces.split(s.trim(), -1);
            if (split.length == 2 && split[1].startsWith(DevPath.DEV)) {
                physicalVolumesMap.computeIfAbsent(split[0], k -> new HashSet<>()).add(split[1]);
            }
        }
        return physicalVolumesMap;
    }

    /**
     * The udev attributes of one block device, read by the bindings so the device-mapper filtering and volume-group
     * assembly can be shared. Any field may be null if udev did not report that property.
     */
    public static final class UdevBlockDevice {
        private final @Nullable String syspath;
        private final @Nullable String devnode;
        private final @Nullable String uuid;
        private final @Nullable String vgName;
        private final @Nullable String lvName;

        /**
         * Creates a block device record.
         *
         * @param syspath the sysfs path of the device, or {@code null} if udev reported none
         * @param devnode the device node path, e.g. {@code /dev/dm-0}, or {@code null} if udev reported none
         * @param uuid    the {@code DM_UUID} property, or {@code null} if absent
         * @param vgName  the {@code DM_VG_NAME} property, or {@code null} if absent
         * @param lvName  the {@code DM_LV_NAME} property, or {@code null} if absent
         */
        public UdevBlockDevice(@Nullable String syspath, @Nullable String devnode, @Nullable String uuid,
                @Nullable String vgName, @Nullable String lvName) {
            this.syspath = syspath;
            this.devnode = devnode;
            this.uuid = uuid;
            this.vgName = vgName;
            this.lvName = lvName;
        }
    }

    /**
     * Creates the binding's volume group type.
     */
    @FunctionalInterface
    public interface LogicalVolumeGroupFactory {
        /**
         * Creates a volume group.
         *
         * @param name  the volume group name
         * @param lvMap the logical volume map
         * @param pvSet the physical volume set
         * @return the volume group
         */
        LogicalVolumeGroup create(String name, Map<String, Set<String>> lvMap, Set<String> pvSet);
    }

    /**
     * Assembles volume groups from enumerated block devices, keeping only the device-mapper devices that LVM owns and
     * reading each one's physical volumes from its sysfs {@code slaves} directory.
     *
     * @param devices the block devices reported by udev
     * @param factory creates the binding's volume group type
     * @return the volume groups, never null
     */
    protected static List<LogicalVolumeGroup> buildLogicalVolumeGroups(List<UdevBlockDevice> devices,
            LogicalVolumeGroupFactory factory) {
        return buildLogicalVolumeGroups(devices, queryPhysicalVolumes(), factory);
    }

    /**
     * Assembles volume groups from enumerated block devices and an already-queried physical volume map. Package-private
     * so tests can supply the map instead of running {@code pvs}.
     *
     * @param devices            the block devices reported by udev
     * @param physicalVolumesMap map of VG name to set of PV device paths, mutated as devices are processed
     * @param factory            creates the binding's volume group type
     * @return the volume groups, never null
     */
    static List<LogicalVolumeGroup> buildLogicalVolumeGroups(List<UdevBlockDevice> devices,
            Map<String, Set<String>> physicalVolumesMap, LogicalVolumeGroupFactory factory) {
        Map<String, Map<String, Set<String>>> logicalVolumesMap = new HashMap<>();
        for (UdevBlockDevice device : devices) {
            if (device.devnode == null || !device.devnode.startsWith(DevPath.DM) || device.uuid == null
                    || !device.uuid.startsWith("LVM-") || Util.isBlank(device.vgName) || Util.isBlank(device.lvName)) {
                continue;
            }
            // The isBlank guards above already establish these, but a predicate does not narrow for the analyzer;
            // the normalizer's return type does.
            String vgName = ParseUtil.getStringValueOrEmpty(device.vgName);
            String lvName = ParseUtil.getStringValueOrEmpty(device.lvName);
            Map<String, Set<String>> lvMapForGroup = logicalVolumesMap.computeIfAbsent(vgName, k -> new HashMap<>());
            Set<String> pvSetForGroup = physicalVolumesMap.computeIfAbsent(vgName, k -> new HashSet<>());
            File[] slaves = new File(device.syspath + "/slaves").listFiles();
            if (slaves != null) {
                for (File f : slaves) {
                    String pvName = DevPath.DEV + f.getName();
                    lvMapForGroup.computeIfAbsent(lvName, k -> new HashSet<>()).add(pvName);
                    pvSetForGroup.add(pvName);
                }
            }
        }
        List<LogicalVolumeGroup> lvgList = new ArrayList<>();
        for (Entry<String, Map<String, Set<String>>> entry : logicalVolumesMap.entrySet()) {
            // Every key here was added to physicalVolumesMap above, but default rather than risk a null set reaching
            // the immutable-copy constructor.
            lvgList.add(factory.create(entry.getKey(), entry.getValue(),
                    physicalVolumesMap.getOrDefault(entry.getKey(), Collections.emptySet())));
        }
        return lvgList;
    }
}
