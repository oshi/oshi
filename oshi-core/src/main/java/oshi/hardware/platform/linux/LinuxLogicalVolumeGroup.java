/**
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
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jna.platform.linux.Udev; // NOSONAR squid:S1191

import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.AbstractLogicalVolumeGroup;

public class LinuxLogicalVolumeGroup extends AbstractLogicalVolumeGroup {

    private static final String BLOCK = "block";
    private static final String DM_UUID = "DM_UUID";
    private static final String DM_VG_NAME = "DM_VG_NAME";
    private static final String DM_LV_NAME = "DM_LV_NAME";

    private LinuxLogicalVolumeGroup(String name, Map<String, List<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        Map<String, Map<String, List<String>>> logicalVolumesMap = new HashMap<>();
        Map<String, Set<String>> physicalVolumesMap = new HashMap<>();

        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            Udev.UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem(BLOCK);
                enumerate.scanDevices();
                for (Udev.UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    Udev.UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            String devnode = device.getDevnode();
                            if (devnode != null && devnode.startsWith("/dev/dm")) {
                                String uuid = device.getPropertyValue(DM_UUID);
                                if (uuid != null && uuid.startsWith("LVM-")) {
                                    String vgName = device.getPropertyValue(DM_VG_NAME);
                                    Map<String, List<String>> lvMapForGroup = logicalVolumesMap.getOrDefault(vgName,
                                            new HashMap<>());
                                    logicalVolumesMap.put(vgName, lvMapForGroup);
                                    Set<String> pvSetForGroup = physicalVolumesMap.getOrDefault(vgName,
                                            new HashSet<>());
                                    physicalVolumesMap.put(vgName, pvSetForGroup);

                                    String lvName = device.getPropertyValue(DM_LV_NAME);
                                    File slavesDir = new File(syspath + "/slaves");
                                    File[] slaves = slavesDir.listFiles();
                                    if (slaves != null) {
                                        for (File f : slaves) {
                                            String pvName = f.getName();
                                            lvMapForGroup.computeIfAbsent(lvName, k -> new ArrayList<>()).add(pvName);
                                            pvSetForGroup.add(pvName);
                                        }
                                    }
                                }
                            }
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
        List<LogicalVolumeGroup> vglist = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<String>>> entry : logicalVolumesMap.entrySet()) {
            vglist.add(new LinuxLogicalVolumeGroup(entry.getKey(), entry.getValue(),
                    physicalVolumesMap.get(entry.getKey())));
        }
        return vglist;
    }
}
