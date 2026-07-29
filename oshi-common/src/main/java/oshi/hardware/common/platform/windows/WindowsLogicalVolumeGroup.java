/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

/**
 * Windows implementation of LogicalVolumeGroup, mapping Storage Spaces storage pools to volume groups. Storage Spaces
 * requires Windows 8 or Server 2012.
 * <p>
 * The correlation logic lives here rather than in the bindings because the {@code MSFT_Storage} queries return
 * backend-neutral {@link WmiResult} values; only the COM plumbing that runs them differs between the bindings.
 */
public class WindowsLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    private static final Pattern SP_OBJECT_ID = Pattern.compile(".*ObjectId=.*SP:(\\{.*\\}).*");
    private static final Pattern PD_OBJECT_ID = Pattern.compile(".*ObjectId=.*PD:(\\{.*\\}).*");
    private static final Pattern VD_OBJECT_ID = Pattern.compile(".*ObjectId=.*VD:(\\{.*\\})(\\{.*\\}).*");

    /**
     * Creates a WindowsLogicalVolumeGroup.
     *
     * @param name  the volume group name
     * @param lvMap the logical volume map
     * @param pvSet the physical volume set
     */
    protected WindowsLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
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
     * Correlates the Storage Spaces query results into volume groups.
     *
     * @param sp      the storage pool query result
     * @param vds     the virtual disk query result
     * @param pds     the physical disk query result
     * @param sppd    the storage pool to physical disk mapping query result
     * @param factory creates the binding's volume group type
     * @return the volume groups, never null
     */
    protected static List<LogicalVolumeGroup> buildFromWmi(WmiResult<StoragePoolProperty> sp,
            WmiResult<VirtualDiskProperty> vds, WmiResult<PhysicalDiskProperty> pds,
            WmiResult<StoragePoolToPhysicalDiskProperty> sppd, LogicalVolumeGroupFactory factory) {
        // Get all the Virtual Disks
        Map<String, String> vdMap = new HashMap<>();
        int count = vds.getResultCount();
        for (int i = 0; i < count; i++) {
            String vdObjectId = WmiUtil.getString(vds, VirtualDiskProperty.OBJECTID, i);
            Matcher m = VD_OBJECT_ID.matcher(vdObjectId);
            if (m.matches()) {
                vdObjectId = m.group(2) + " " + m.group(1);
            }
            // Store key with SP|VD
            vdMap.put(vdObjectId, WmiUtil.getString(vds, VirtualDiskProperty.FRIENDLYNAME, i));
        }

        // Get all the Physical Disks
        Map<String, Pair<String, String>> pdMap = new HashMap<>();
        count = pds.getResultCount();
        for (int i = 0; i < count; i++) {
            String pdObjectId = WmiUtil.getString(pds, PhysicalDiskProperty.OBJECTID, i);
            Matcher m = PD_OBJECT_ID.matcher(pdObjectId);
            if (m.matches()) {
                pdObjectId = m.group(1);
            }
            // Store key with PD
            pdMap.put(pdObjectId, new Pair<>(WmiUtil.getString(pds, PhysicalDiskProperty.FRIENDLYNAME, i),
                    WmiUtil.getString(pds, PhysicalDiskProperty.PHYSICALLOCATION, i)));
        }

        // Get the Storage Pool to Physical Disk mapping
        Map<String, String> sppdMap = new HashMap<>();
        count = sppd.getResultCount();
        for (int i = 0; i < count; i++) {
            // Ref string contains object id, will do partial match later
            String spObjectId = WmiUtil.getRefString(sppd, StoragePoolToPhysicalDiskProperty.STORAGEPOOL, i);
            Matcher m = SP_OBJECT_ID.matcher(spObjectId);
            if (m.matches()) {
                spObjectId = m.group(1);
            }
            String pdObjectId = WmiUtil.getRefString(sppd, StoragePoolToPhysicalDiskProperty.PHYSICALDISK, i);
            m = PD_OBJECT_ID.matcher(pdObjectId);
            if (m.matches()) {
                pdObjectId = m.group(1);
            }
            sppdMap.put(spObjectId + " " + pdObjectId, pdObjectId);
        }

        // Finally process the storage pools
        List<LogicalVolumeGroup> lvgList = new ArrayList<>();
        count = sp.getResultCount();
        for (int i = 0; i < count; i++) {
            // Name
            String name = WmiUtil.getString(sp, StoragePoolProperty.FRIENDLYNAME, i);
            // Parse object ID to match
            String spObjectId = WmiUtil.getString(sp, StoragePoolProperty.OBJECTID, i);
            Matcher m = SP_OBJECT_ID.matcher(spObjectId);
            if (m.matches()) {
                spObjectId = m.group(1);
            }
            // find matching physical and logical volumes
            Set<String> physicalVolumeSet = new HashSet<>();
            for (Entry<String, String> entry : sppdMap.entrySet()) {
                if (entry.getKey().contains(spObjectId)) {
                    String pdObjectId = entry.getValue();
                    Pair<String, String> nameLoc = pdMap.get(pdObjectId);
                    if (nameLoc != null) {
                        physicalVolumeSet.add(nameLoc.getA() + " @ " + nameLoc.getB());
                    }
                }
            }
            // find matching logical volume
            Map<String, Set<String>> logicalVolumeMap = new HashMap<>();
            for (Entry<String, String> entry : vdMap.entrySet()) {
                if (entry.getKey().contains(spObjectId)) {
                    String vdObjectId = ParseUtil.whitespaces.split(entry.getKey())[0];
                    logicalVolumeMap.put(entry.getValue() + " " + vdObjectId, physicalVolumeSet);
                }
            }
            // Add to list
            lvgList.add(factory.create(name, logicalVolumeMap, physicalVolumeSet));
        }
        return lvgList;
    }
}
