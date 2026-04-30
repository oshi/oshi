/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskProperty;
import oshi.driver.windows.wmi.MSFTStorageFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.ffm.windows.com.FfmComException;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;

final class WindowsLogicalVolumeGroupFFM extends AbstractLogicalVolumeGroup {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsLogicalVolumeGroupFFM.class);

    private static final Pattern SP_OBJECT_ID = Pattern.compile(".*ObjectId=.*SP:(\\{.*\\}).*");
    private static final Pattern PD_OBJECT_ID = Pattern.compile(".*ObjectId=.*PD:(\\{.*\\}).*");
    private static final Pattern VD_OBJECT_ID = Pattern.compile(".*ObjectId=.*VD:(\\{.*\\})(\\{.*\\}).*");

    private static final boolean IS_WINDOWS8_OR_GREATER = VersionHelpersFFM.IsWindows8OrGreater();

    WindowsLogicalVolumeGroupFFM(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        if (!IS_WINDOWS8_OR_GREATER) {
            return Collections.emptyList();
        }
        WmiQueryHandlerFFM h = Objects.requireNonNull(WmiQueryHandlerFFM.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            WmiResult<StoragePoolProperty> sp = MSFTStorageFFM.queryStoragePools(h);
            int count = sp.getResultCount();
            if (count == 0) {
                return Collections.emptyList();
            }

            Map<String, String> vdMap = new HashMap<>();
            WmiResult<VirtualDiskProperty> vds = MSFTStorageFFM.queryVirtualDisks(h);
            count = vds.getResultCount();
            for (int i = 0; i < count; i++) {
                String vdObjectId = WmiUtilFFM.getString(vds, VirtualDiskProperty.OBJECTID, i);
                Matcher m = VD_OBJECT_ID.matcher(vdObjectId);
                if (m.matches()) {
                    vdObjectId = m.group(2) + " " + m.group(1);
                }
                vdMap.put(vdObjectId, WmiUtilFFM.getString(vds, VirtualDiskProperty.FRIENDLYNAME, i));
            }

            Map<String, Pair<String, String>> pdMap = new HashMap<>();
            WmiResult<PhysicalDiskProperty> pds = MSFTStorageFFM.queryPhysicalDisks(h);
            count = pds.getResultCount();
            for (int i = 0; i < count; i++) {
                String pdObjectId = WmiUtilFFM.getString(pds, PhysicalDiskProperty.OBJECTID, i);
                Matcher m = PD_OBJECT_ID.matcher(pdObjectId);
                if (m.matches()) {
                    pdObjectId = m.group(1);
                }
                pdMap.put(pdObjectId, new Pair<>(WmiUtilFFM.getString(pds, PhysicalDiskProperty.FRIENDLYNAME, i),
                        WmiUtilFFM.getString(pds, PhysicalDiskProperty.PHYSICALLOCATION, i)));
            }

            Map<String, String> sppdMap = new HashMap<>();
            WmiResult<StoragePoolToPhysicalDiskProperty> sppd = MSFTStorageFFM.queryStoragePoolPhysicalDisks(h);
            count = sppd.getResultCount();
            for (int i = 0; i < count; i++) {
                String spObjectId = WmiUtilFFM.getRefString(sppd, StoragePoolToPhysicalDiskProperty.STORAGEPOOL, i);
                Matcher m = SP_OBJECT_ID.matcher(spObjectId);
                if (m.matches()) {
                    spObjectId = m.group(1);
                }
                String pdObjectId = WmiUtilFFM.getRefString(sppd, StoragePoolToPhysicalDiskProperty.PHYSICALDISK, i);
                m = PD_OBJECT_ID.matcher(pdObjectId);
                if (m.matches()) {
                    pdObjectId = m.group(1);
                }
                sppdMap.put(spObjectId + " " + pdObjectId, pdObjectId);
            }

            List<LogicalVolumeGroup> lvgList = new ArrayList<>();
            count = sp.getResultCount();
            for (int i = 0; i < count; i++) {
                String name = WmiUtilFFM.getString(sp, StoragePoolProperty.FRIENDLYNAME, i);
                String spObjectId = WmiUtilFFM.getString(sp, StoragePoolProperty.OBJECTID, i);
                Matcher m = SP_OBJECT_ID.matcher(spObjectId);
                if (m.matches()) {
                    spObjectId = m.group(1);
                }
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
                Map<String, Set<String>> logicalVolumeMap = new HashMap<>();
                for (Entry<String, String> entry : vdMap.entrySet()) {
                    if (entry.getKey().contains(spObjectId)) {
                        String vdObjectId = ParseUtil.whitespaces.split(entry.getKey())[0];
                        logicalVolumeMap.put(entry.getValue() + " " + vdObjectId, physicalVolumeSet);
                    }
                }
                lvgList.add(new WindowsLogicalVolumeGroupFFM(name, logicalVolumeMap, physicalVolumeSet));
            }
            return lvgList;
        } catch (FfmComException e) {
            LOG.warn("COM exception: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }
}
