/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.VersionHelpers; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.driver.windows.wmi.MSFTStorage;
import oshi.driver.windows.wmi.MSFTStorage.PhysicalDiskProperty;
import oshi.driver.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.windows.wmi.MSFTStorage.StoragePoolToPhysicalDiskProperty;
import oshi.driver.windows.wmi.MSFTStorage.VirtualDiskProperty;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

final class WindowsLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsLogicalVolumeGroup.class);

    private static final Pattern SP_OBJECT_ID = Pattern.compile(".*ObjectId=.*SP:(\\{.*\\}).*");
    private static final Pattern PD_OBJECT_ID = Pattern.compile(".*ObjectId=.*PD:(\\{.*\\}).*");
    private static final Pattern VD_OBJECT_ID = Pattern.compile(".*ObjectId=.*VD:(\\{.*\\})(\\{.*\\}).*");

    private static final boolean IS_WINDOWS8_OR_GREATER = VersionHelpers.IsWindows8OrGreater();

    WindowsLogicalVolumeGroup(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        // Storage Spaces requires Windows 8 or Server 2012
        if (!IS_WINDOWS8_OR_GREATER) {
            return Collections.emptyList();
        }
        WmiQueryHandler h = WmiQueryHandler.createInstance();
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            // Query Storage Pools first, so we can skip other queries if we have no pools
            WmiResult<StoragePoolProperty> sp = MSFTStorage.queryStoragePools(h);
            int count = sp.getResultCount();
            if (count == 0) {
                return Collections.emptyList();
            }
            // We have storage pool(s) but now need to gather other info

            // Get all the Virtual Disks
            Map<String, String> vdMap = new HashMap<>();
            WmiResult<VirtualDiskProperty> vds = MSFTStorage.queryVirtualDisks(h);
            count = vds.getResultCount();
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
            WmiResult<PhysicalDiskProperty> pds = MSFTStorage.queryPhysicalDisks(h);
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

            // Get the Storage Pool to Physical Disk mappping
            Map<String, String> sppdMap = new HashMap<>();
            WmiResult<StoragePoolToPhysicalDiskProperty> sppd = MSFTStorage.queryStoragePoolPhysicalDisks(h);
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
                lvgList.add(new WindowsLogicalVolumeGroup(name, logicalVolumeMap, physicalVolumeSet));
            }

            return lvgList;
        } catch (COMException e) {
            LOG.warn("COM exception: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }
}
