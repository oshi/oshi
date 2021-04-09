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

import static com.sun.jna.platform.win32.SetupApi.DIGCF_DEVICEINTERFACE;
import static com.sun.jna.platform.win32.SetupApi.DIGCF_PRESENT;
import static com.sun.jna.platform.win32.WinBase.INVALID_HANDLE_VALUE;
import static com.sun.jna.platform.win32.WinError.ERROR_INSUFFICIENT_BUFFER;
import static com.sun.jna.platform.win32.WinError.ERROR_NO_MORE_ITEMS;
import static com.sun.jna.platform.win32.WinError.ERROR_SUCCESS;
import static oshi.jna.platform.windows.Cfgmgr32.CM_DRP_DEVICEDESC;
import static oshi.jna.platform.windows.SetupApi.SPDRP_CLASS;
import static oshi.jna.platform.windows.SetupApi.SPDRP_COMPATIBLEIDS;
import static oshi.jna.platform.windows.SetupApi.SPDRP_DEVICEDESC;
import static oshi.jna.platform.windows.SetupApi.SPDRP_DRIVER;
import static oshi.jna.platform.windows.SetupApi.SPDRP_FRIENDLYNAME;
import static oshi.jna.platform.windows.SetupApi.SPDRP_HARDWAREID;
import static oshi.jna.platform.windows.SetupApi.SPDRP_LOCATION_INFORMATION;
import static oshi.jna.platform.windows.SetupApi.SPDRP_MFG;
import static oshi.jna.platform.windows.SetupApi.SPDRP_PHYSICAL_DEVICE_OBJECT_NAME;
import static oshi.jna.platform.windows.SetupApi.SPDRP_SERVICE;
import static oshi.jna.platform.windows.Winioctl.IOCTL_USB_GET_NODE_CONNECTION_DRIVERKEY_NAME;
import static oshi.jna.platform.windows.Winioctl.IOCTL_USB_GET_NODE_CONNECTION_INFORMATION;
import static oshi.jna.platform.windows.Winioctl.IOCTL_USB_GET_NODE_CONNECTION_NAME;
import static oshi.jna.platform.windows.Winioctl.IOCTL_USB_GET_NODE_INFORMATION;
import static oshi.jna.platform.windows.Winioctl.IOCTL_USB_GET_ROOT_HUB_NAME;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.Cfgmgr32Util;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.jna.platform.windows.Cfgmgr32;
import oshi.jna.platform.windows.SetupApi;
import oshi.jna.platform.windows.SetupApi.SP_DEVICE_INTERFACE_DETAIL_DATA;
import oshi.jna.platform.windows.Winioctl.USB_NODE_CONNECTION_DRIVERKEY_NAME;
import oshi.jna.platform.windows.Winioctl.USB_NODE_CONNECTION_INFORMATION;
import oshi.jna.platform.windows.Winioctl.USB_NODE_CONNECTION_NAME;
import oshi.jna.platform.windows.Winioctl.USB_NODE_INFORMATION;
import oshi.jna.platform.windows.Winioctl.USB_ROOT_HUB_NAME;
import oshi.util.tuples.Triplet;

/**
 * Utility to query Device Tree
 */
@ThreadSafe
public final class DeviceTree {

    private static final SetupApi SA = SetupApi.INSTANCE;
    private static final Kernel32 K32 = Kernel32.INSTANCE;
    private static final Cfgmgr32 C32 = Cfgmgr32.INSTANCE;

    private static final GUID GUID_DEVINTERFACE_USB_HOST_CONTROLLER = new GUID(
            "{3ABF6F2D-71C4-462A-8A92-1E6861E6AF27}");
    private static final GUID GUID_DEVINTERFACE_USB_HUB = new GUID("{F18A0E88-C30C-11D0-8815-00A0C906BED8}");
    private static final GUID GUID_DEVINTERFACE_USB_DEVICE = new GUID("{A5DCBF10-6530-11D2-901F-00C04FB951ED}");

    private static final String DEVICE_PATH_PREFIX = "\\\\.\\";

    private DeviceTree() {
    }

    public static void main(String[] args) {
        Map<String, Triplet<String, String, String>> deviceMap = enumerateDevices(GUID_DEVINTERFACE_USB_HOST_CONTROLLER,
                GUID_DEVINTERFACE_USB_HUB, GUID_DEVINTERFACE_USB_DEVICE);
        System.out.println("CM Device Tree");
        queryUSBTree();
//        queryUSB();
        System.out.println();
        System.out.println("IOCTL");
        enumerateControllers(deviceMap);
    }

    // This will be main iteration for Windows USB
    public static void queryUSBTree() {
        // Get device IDs for the USB Host Controllers
        HANDLE hDevInfo = SA.SetupDiGetClassDevs(GUID_DEVINTERFACE_USB_HOST_CONTROLLER, null, null,
                DIGCF_DEVICEINTERFACE | DIGCF_PRESENT);
        if (hDevInfo == INVALID_HANDLE_VALUE) {
            System.out.println("Invalid Handle Value");
            return;
        }
        try {
            // Enumerate Device Info using BFS queue
            Queue<Integer> deviceTree = new ArrayDeque<>();
            Map<Integer, String> deviceMap = new HashMap<>();
            Map<Integer, Integer> parentMap = new HashMap<>();
            SP_DEVINFO_DATA devInfoData = new SP_DEVINFO_DATA();
            devInfoData.cbSize = devInfoData.size();
            for (int i = 0; SA.SetupDiEnumDeviceInfo(hDevInfo, i, devInfoData); i++) {
                deviceTree.add(devInfoData.DevInst);
                int parent = 0;
                IntByReference child = new IntByReference();
                IntByReference sibling = new IntByReference();
                while (!deviceTree.isEmpty()) {
                    parent = deviceTree.poll();
                    String deviceId = Cfgmgr32Util.CM_Get_Device_ID(parent);
                    deviceMap.put(parent, deviceId);
                    if (ERROR_SUCCESS == C32.CM_Get_Child(child, parent, 0)) {
                        parentMap.put(child.getValue(), parent);
                        deviceTree.add(child.getValue());
                        while (ERROR_SUCCESS == C32.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                            parentMap.put(sibling.getValue(), parent);
                            deviceTree.add(sibling.getValue());
                            child.setValue(sibling.getValue());
                        }
                    }
                }
            }
            // Look for output without parents, these are top of tree
            Set<Integer> devices = deviceMap.keySet().stream().filter(k -> !parentMap.containsKey(k))
                    .collect(Collectors.toSet());
            // Recursively print tree (TODO: build a map and return)
            printTree(deviceMap, parentMap, devices, 0);
        } finally {
            SA.SetupDiDestroyDeviceInfoList(hDevInfo);
        }
    }

    private static void printTree(Map<Integer, String> deviceMap, Map<Integer, Integer> parentMap, Set<Integer> devices,
            int indent) {
        for (Integer device : devices) {
            Memory buf = new Memory(256);
            IntByReference size = new IntByReference(256);
            C32.CM_Get_DevNode_Registry_Property(device, CM_DRP_DEVICEDESC, null, buf, size, 0);
            String desc = buf.getWideString(0);
            System.out.println("    ".repeat(indent) + desc + " ... " + deviceMap.get(device));
            // TODO:
            // Get the class (parse deviceID first chars)
            // if class is HID get info from first child
            // buffer MAX_PATH
            // CM_Get_DevNode_Registry_Property(child, CM_DRP_CLASS, null, buff, len, null)
            // CM_Get_DevNode_Registry_Property(child, CM_DRP_DEVICEDESC, null, buff, len,
            // null)
            // FOR CONTROLLERS:
            // IOCTL_USB_USER_REQUEST --> USBUSER_CONTROLLER_INFO_0
            // set USBUSER_CONTROLLER_INFO_0.Header.UsbUserRequest to
            // USBUSER_GET_CONTROLLER_INFO_0
            //
            // See:
            // https://www.codeproject.com/Articles/6445/Enumerate-Installed-Devices-Using-Setup-API
            // CM_DRP_FRIENDLYNAME, CM_DRP_DEVICEDESC
            //
            // Device ID for USB is USB\VID_xxxx&PID_xxxx& serial number if
            // configs.numInterfaces == 1 (otherwise contains MI_xx)
            // Serial also in serialnum&LUN(hex)&SILO(4hex)_SiloIdx(4hex
            // DEVSERIALNUMBERINDEX = 10

            Set<Integer> children = parentMap.entrySet().stream().filter(e -> e.getValue().equals(device))
                    .map(e -> e.getKey()).collect(Collectors.toSet());
            printTree(deviceMap, parentMap, children, indent + 1);
        }
    }

    private static void enumerateControllers(Map<String, Triplet<String, String, String>> deviceMap) {
        HANDLE hDevInfo = SA.SetupDiGetClassDevs(GUID_DEVINTERFACE_USB_HOST_CONTROLLER, null, null,
                DIGCF_DEVICEINTERFACE | DIGCF_PRESENT);
        if (hDevInfo != INVALID_HANDLE_VALUE) {
            try {
                // Enumerate interfaces
                SP_DEVICE_INTERFACE_DATA devIntfData = new SP_DEVICE_INTERFACE_DATA();
                devIntfData.cbSize = devIntfData.size();
                int i = 0;
                SA.SetupDiEnumDeviceInterfaces(hDevInfo, null, GUID_DEVINTERFACE_USB_HOST_CONTROLLER, i, devIntfData);
                while (K32.GetLastError() != ERROR_NO_MORE_ITEMS) {
                    // Get required size to hold SP_DEVICE_INTERFACE_DETAIL_DATA
                    IntByReference requiredSize = new IntByReference();
                    SA.SetupDiGetDeviceInterfaceDetail(hDevInfo, devIntfData, null, 0, requiredSize, null);
                    if (K32.GetLastError() != ERROR_INSUFFICIENT_BUFFER || requiredSize.getValue() == 0) {
                        System.out.println("Failed to get intetrface detail size");
                        return;
                    }
                    // Allocate the structure
                    int size = requiredSize.getValue();
                    SP_DEVICE_INTERFACE_DETAIL_DATA devIntfDetailData = new SP_DEVICE_INTERFACE_DETAIL_DATA(size);
                    if (SA.SetupDiGetDeviceInterfaceDetail(hDevInfo, devIntfData, devIntfDetailData.getPointer(), size,
                            null, null)) {
                        devIntfDetailData.read();
                        String devPath = Native.toString(devIntfDetailData.DevicePath);
                        System.out.println(devPath);
                        HANDLE h = K32.CreateFile(devPath, 0, 0, null, WinNT.OPEN_EXISTING, 0, null);
                        if (h != null) {
                            try {
                                IntByReference lpbytesReturned = new IntByReference();
                                USB_ROOT_HUB_NAME usbRootHubName = new USB_ROOT_HUB_NAME();
                                K32.DeviceIoControl(h, IOCTL_USB_GET_ROOT_HUB_NAME, null, 0,
                                        usbRootHubName.getPointer(), usbRootHubName.ActualLength, lpbytesReturned,
                                        null);
                                usbRootHubName.read();
                                // Allocate and call again
                                if (usbRootHubName.ActualLength > 0) {
                                    usbRootHubName.resizeBuffer();
                                    if (K32.DeviceIoControl(h, IOCTL_USB_GET_ROOT_HUB_NAME, null, 0,
                                            usbRootHubName.getPointer(), usbRootHubName.ActualLength, lpbytesReturned,
                                            null)) {
                                        usbRootHubName.read();
                                        String rootHubPath = DEVICE_PATH_PREFIX
                                                + Native.toString(usbRootHubName.RootHubName);
                                        System.out.println("|-- " + rootHubPath);
                                        HANDLE rh = K32.CreateFile(rootHubPath, 0, 0, null, WinNT.OPEN_EXISTING, 0,
                                                null);
                                        if (rh != null) {
                                            try {
                                                USB_NODE_INFORMATION nodeInfo = new USB_NODE_INFORMATION();
                                                K32.DeviceIoControl(rh, IOCTL_USB_GET_NODE_INFORMATION,
                                                        nodeInfo.getPointer(), nodeInfo.size(), nodeInfo.getPointer(),
                                                        nodeInfo.size(), lpbytesReturned, null);
                                                nodeInfo.read();
                                                enumerateHubPorts(rh,
                                                        nodeInfo.HubInformation.HubDescriptor.bNumberOfPorts, deviceMap,
                                                        1);
                                            } finally {
                                                K32.CloseHandle(rh);
                                            }
                                        }
                                    }
                                }
                            } finally {
                                K32.CloseHandle(h);
                            }
                        }
                    } else {
                        System.out.println("DIDD failed " + K32.GetLastError());
                    }
                    SA.SetupDiEnumDeviceInterfaces(hDevInfo, null, GUID_DEVINTERFACE_USB_HOST_CONTROLLER, ++i,
                            devIntfData);
                }

            } finally {
                SA.SetupDiDestroyDeviceInfoList(hDevInfo);
            }
        }
    }

    private static void enumerateHubPorts(HANDLE h, int numPorts,
            Map<String, Triplet<String, String, String>> deviceMap, int indent) {
        IntByReference lpbytesReturned = new IntByReference();
        // Port 0 is system, we iterate starting at next one
        for (int port = 1; port <= numPorts; port++) {
            USB_NODE_CONNECTION_INFORMATION connectionInfo = new USB_NODE_CONNECTION_INFORMATION(port);
            if (K32.DeviceIoControl(h, IOCTL_USB_GET_NODE_CONNECTION_INFORMATION, connectionInfo.getPointer(),
                    connectionInfo.size(), connectionInfo.getPointer(), connectionInfo.size(), lpbytesReturned, null)) {
                connectionInfo.read();
                if (connectionInfo.ConnectionIndex > 0) {
                    byte serialNumber = connectionInfo.DeviceDescriptor.iSerialNumber;
                    System.out.print("    ".repeat(indent) + "|-- " + port
                            + ": vid="
                            + String.format("0x%04x", connectionInfo.DeviceDescriptor.idVendor) + ", pid="
                            + String.format("0x%04x", connectionInfo.DeviceDescriptor.idProduct));
                    USB_NODE_CONNECTION_DRIVERKEY_NAME driverKeyName = new USB_NODE_CONNECTION_DRIVERKEY_NAME(port);

                    // TODO: do this same query for the controller
                    // Call to get size
                    K32.DeviceIoControl(h, IOCTL_USB_GET_NODE_CONNECTION_DRIVERKEY_NAME, driverKeyName.getPointer(),
                            driverKeyName.size(), driverKeyName.getPointer(), driverKeyName.size(), lpbytesReturned,
                            null);
                    driverKeyName.read();
                    driverKeyName.resizeBuffer();
                    // Call again
                    if (K32.DeviceIoControl(h, IOCTL_USB_GET_NODE_CONNECTION_DRIVERKEY_NAME, driverKeyName.getPointer(),
                            driverKeyName.ActualLength, driverKeyName.getPointer(), driverKeyName.ActualLength,
                            lpbytesReturned, null)) {
                        driverKeyName.read();
                        Triplet<String, String, String> deviceInfo = deviceMap
                                .get(Native.toString(driverKeyName.DriverKeyName));
                        if (deviceInfo != null) {
                            System.out.println(
                                    " --> " + deviceInfo.getA() + " [" + deviceInfo.getB() + "] " + deviceInfo.getC());
                        } else {
                            System.out.println();
                        }
                    }
                    if (serialNumber > 0) {
                        System.out.println("    ".repeat(indent + 1) + "Has serial number");
                    }
                    if (connectionInfo.DeviceIsHub > 0) {
                        USB_NODE_CONNECTION_NAME nodeName = new USB_NODE_CONNECTION_NAME(port);
                        K32.DeviceIoControl(h, IOCTL_USB_GET_NODE_CONNECTION_NAME, nodeName.getPointer(),
                                nodeName.size(), nodeName.getPointer(), nodeName.size(), lpbytesReturned, null);
                        nodeName.read();
                        nodeName.resizeBuffer();
                        // Call again
                        if (K32.DeviceIoControl(h, IOCTL_USB_GET_NODE_CONNECTION_NAME, nodeName.getPointer(),
                                nodeName.ActualLength, nodeName.getPointer(), nodeName.ActualLength, lpbytesReturned,
                                null)) {
                            nodeName.read();
                            String nodePath = DEVICE_PATH_PREFIX + Native.toString(nodeName.NodeName);
                            HANDLE nh = K32.CreateFile(nodePath, 0, 0, null, WinNT.OPEN_EXISTING, 0, null);
                            if (nh != null) {
                                try {
                                    USB_NODE_INFORMATION nodeInfo = new USB_NODE_INFORMATION();
                                    K32.DeviceIoControl(nh, IOCTL_USB_GET_NODE_INFORMATION, nodeInfo
                                            .getPointer(),
                                            nodeInfo.size(), nodeInfo.getPointer(), nodeInfo.size(), lpbytesReturned,
                                            null);
                                    nodeInfo.read();
                                    enumerateHubPorts(nh, nodeInfo.HubInformation.HubDescriptor.bNumberOfPorts,
                                            deviceMap, indent + 1);
                                } finally {
                                    K32.CloseHandle(nh);
                                }
                            }
                        }
                    }
                }
            }
            // TODO: Call IOCTL_USB_GET_NODE_CONNECTION_INFORMATION
            // Returns USB_NODE_CONNECTION_INFORMATION
            // Has a USB_DEVICE_DESCRIPTOR member
            // has vendor, product
            // has index of string manufacturer, product, serial #
            // check if any nonzero
            // pass this device descriptor to
            // IOCTL_USB_GET_DESCRIPTOR_FROM_NODE_CONNECTION
            // get supported languages passing 0 0, that gets all the strings
            // pass index and language offset to get string
            // for each of manuf, prod, ser

        }

    }

    private static Map<String, Triplet<String, String, String>> enumerateDevices(GUID... guids) {
        Map<String, Triplet<String, String, String>> deviceMap = new HashMap<>();
        for (GUID guid : guids) {
            HANDLE hDevInfo = SA.SetupDiGetClassDevs(guid, null, null, DIGCF_DEVICEINTERFACE | DIGCF_PRESENT);
            if (hDevInfo != INVALID_HANDLE_VALUE) {
                try {
                    SP_DEVINFO_DATA devInfoData = new SP_DEVINFO_DATA();
                    devInfoData.cbSize = devInfoData.size();
                    // Enumerate
                    int i = 0;
                    SA.SetupDiEnumDeviceInfo(hDevInfo, i, devInfoData);
                    while (K32.GetLastError() != ERROR_NO_MORE_ITEMS) {
                        // The driver is a unique key we can cross reference when iterating the tree
                        // Name is sometimes blank, use desc as a backup
                        String driver = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_DRIVER);
                        if (!driver.isEmpty()) {
                            String name = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_FRIENDLYNAME);
                            String desc = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_DEVICEDESC);
                            // Manufacturer and deviceid
                            String mfg = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_MFG);
                            String id = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_HARDWAREID);
                            // Other stuff we may want for xref later
                            String cid = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_COMPATIBLEIDS);
                            String svc = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_SERVICE);
                            String clas = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_CLASS);
                            String loc = getDeviceRegistryProperty(hDevInfo, devInfoData, SPDRP_LOCATION_INFORMATION);
                            String objname = getDeviceRegistryProperty(hDevInfo, devInfoData,
                                    SPDRP_PHYSICAL_DEVICE_OBJECT_NAME);
                            /*-
                            System.out.println(driver);
                            System.out.println("  " + name + ": " + desc);
                            System.out.println("  " + mfg + ": " + id);
                            System.out.println("  " + cid + " / " + svc + " / " + clas);
                            System.out.println("  " + loc + ": " + objname);
                            System.out.println();
                            */
                            deviceMap.put(driver, new Triplet<>(name.isEmpty() ? desc : name, mfg, id));

                        }
                        SA.SetupDiEnumDeviceInfo(hDevInfo, ++i, devInfoData);
                    }
                } finally {
                    SA.SetupDiDestroyDeviceInfoList(hDevInfo);
                }
            }
        }
        return deviceMap;
    }

    private static String getDeviceRegistryProperty(HANDLE hDevInfo, SP_DEVINFO_DATA devInfoData, int property) {
        IntByReference requiredLength = new IntByReference();
        boolean success = SA.SetupDiGetDeviceRegistryProperty(hDevInfo, devInfoData, property, null, null, 0,
                requiredLength);
        int size = requiredLength.getValue();
        if (size > 0 && (success || K32.GetLastError() == ERROR_INSUFFICIENT_BUFFER)) {
            Memory buffer = new Memory(size);
            if (SA.SetupDiGetDeviceRegistryProperty(hDevInfo, devInfoData, property, null, buffer, size, null)) {
                return buffer.getWideString(0);
            }
        }
        return "";
    }

}
