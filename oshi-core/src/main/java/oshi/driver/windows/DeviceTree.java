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
package oshi.driver.windows;

import static com.sun.jna.platform.win32.SetupApi.DIGCF_DEVICEINTERFACE; // NOSONAR squid:S1191
import static com.sun.jna.platform.win32.SetupApi.DIGCF_PRESENT;
import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinError.ERROR_SUCCESS;
import static oshi.jna.platform.windows.Cfgmgr32.CM_DRP_CLASS;
import static oshi.jna.platform.windows.Cfgmgr32.CM_DRP_DEVICEDESC;
import static oshi.jna.platform.windows.Cfgmgr32.CM_DRP_FRIENDLYNAME;
import static oshi.jna.platform.windows.Cfgmgr32.CM_DRP_MFG;
import static oshi.jna.platform.windows.Cfgmgr32.CM_DRP_SERVICE;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.Cfgmgr32Util;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.windows.Cfgmgr32;
import oshi.util.tuples.Quintet;

/**
 * Utility to query USB devices via Config Manager Device Tree functions
 */
@ThreadSafe
public final class DeviceTree {

    private static final int MAX_PATH = 260;
    private static final SetupApi SA = SetupApi.INSTANCE;
    private static final Cfgmgr32 C32 = Cfgmgr32.INSTANCE;

    private static final GUID GUID_DEVINTERFACE_USB_HOST_CONTROLLER = new GUID(
            "{3ABF6F2D-71C4-462A-8A92-1E6861E6AF27}");

    private DeviceTree() {
    }

    /**
     * Queries USB devices and returns maps representing device tree relationships,
     * name, device ID, and manufacturer
     *
     * @return A {@link Quintet} of maps indexed by node ID, where the key set
     *         represents node IDs for all USB devices, including the controllers.
     *         The first element is a set containing devices with no parents, which
     *         should be the USB Controller Devices. The second element maps each
     *         node ID to its parents, if any. Controllers will not be in the key
     *         set for this map. The third element maps a node ID to a name or
     *         description. The fourth element maps a node id to a device ID. The
     *         fifth element maps a node ID to a manufacturer.
     */
    public static Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> queryUSBDevices() {
        Map<Integer, Integer> parentMap = new HashMap<>();
        Map<Integer, String> nameMap = new HashMap<>();
        Map<Integer, String> deviceIdMap = new HashMap<>();
        Map<Integer, String> mfgMap = new HashMap<>();
        // Get device IDs for the USB Host Controllers
        HANDLE hDevInfo = SA.SetupDiGetClassDevs(GUID_DEVINTERFACE_USB_HOST_CONTROLLER, null, null,
                DIGCF_DEVICEINTERFACE | DIGCF_PRESENT);
        if (hDevInfo != INVALID_HANDLE_VALUE) {
            try {
                // Enumerate Device Info using BFS queue
                Queue<Integer> deviceTree = new ArrayDeque<>();
                SP_DEVINFO_DATA devInfoData = new SP_DEVINFO_DATA();
                devInfoData.cbSize = devInfoData.size();
                Memory buf = new Memory(MAX_PATH);
                IntByReference size = new IntByReference(MAX_PATH);
                for (int i = 0; SA.SetupDiEnumDeviceInfo(hDevInfo, i, devInfoData); i++) {
                    deviceTree.add(devInfoData.DevInst);
                    int node = 0;
                    IntByReference child = new IntByReference();
                    IntByReference sibling = new IntByReference();
                    while (!deviceTree.isEmpty()) {
                        // Process the next device in the queue
                        node = deviceTree.poll();

                        // Save the strings in their maps
                        String deviceId = Cfgmgr32Util.CM_Get_Device_ID(node);
                        deviceIdMap.put(node, deviceId);
                        // Prefer friendly name over desc. If neither, use class (service)
                        String name = getDevNodeProperty(node, CM_DRP_FRIENDLYNAME, buf, size);
                        if (name.isEmpty()) {
                            name = getDevNodeProperty(node, CM_DRP_DEVICEDESC, buf, size);
                        }
                        if (name.isEmpty()) {
                            name = getDevNodeProperty(node, CM_DRP_CLASS, buf, size);
                            String svc = getDevNodeProperty(node, CM_DRP_SERVICE, buf, size);
                            if (!svc.isEmpty()) {
                                name = name + " (" + svc + ")";
                            }
                        }
                        nameMap.put(node, name);
                        mfgMap.put(node, getDevNodeProperty(node, CM_DRP_MFG, buf, size));

                        if (ERROR_SUCCESS == C32.CM_Get_Child(child, node, 0)) {
                            parentMap.put(child.getValue(), node);
                            deviceTree.add(child.getValue());
                            while (ERROR_SUCCESS == C32.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                                parentMap.put(sibling.getValue(), node);
                                deviceTree.add(sibling.getValue());
                                child.setValue(sibling.getValue());
                            }
                        }
                    }
                }
            } finally {
                SA.SetupDiDestroyDeviceInfoList(hDevInfo);
            }
        }
        // Look for output without parents, these are top of tree
        Set<Integer> controllerDevices = deviceIdMap.keySet().stream().filter(k -> !parentMap.containsKey(k))
                .collect(Collectors.toSet());
        return new Quintet<>(controllerDevices, parentMap, nameMap, deviceIdMap, mfgMap);
    }

    private static String getDevNodeProperty(int node, int cmDrp, Memory buf, IntByReference size) {
        buf.clear();
        size.setValue((int) buf.size());
        C32.CM_Get_DevNode_Registry_Property(node, cmDrp, null, buf, size, 0);
        return buf.getWideString(0);
    }
}
