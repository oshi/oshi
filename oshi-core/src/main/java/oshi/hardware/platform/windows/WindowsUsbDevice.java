/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Cfgmgr32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Cfgmgr32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.ptr.IntByReference;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.wmi.Win32DiskDrive;
import oshi.driver.windows.wmi.Win32DiskDrive.DeviceIdProperty;
import oshi.driver.windows.wmi.Win32PnPEntity;
import oshi.driver.windows.wmi.Win32PnPEntity.PnPEntityProperty;
import oshi.driver.windows.wmi.Win32USBController;
import oshi.driver.windows.wmi.Win32USBController.USBControllerProperty;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Windows Usb Device
 */
@Immutable
public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsUsbDevice.class);

    public WindowsUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * {@inheritDoc}
     *
     * @param tree
     *            a boolean.
     * @return an array of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = getUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        // Top level is controllers; they won't be added to the list, but all
        // their connected devices will be
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static List<UsbDevice> getUsbDevices() {
        // Map to build the recursive tree structure
        Map<String, List<String>> deviceTreeMap = new HashMap<>();
        // Track devices seen in the process
        Set<String> devicesSeen = new HashSet<>();

        // Navigate the device tree to track what devices are present
        List<UsbDevice> controllerDevices = new ArrayList<>();
        List<String> controllerDeviceIdList = getControllerDeviceIdList();
        for (String controllerDeviceId : controllerDeviceIdList) {
            putChildrenInDeviceTree(controllerDeviceId, 0, deviceTreeMap, devicesSeen);
        }
        // Map to store information using PNPDeviceID as the key.
        Map<String, Triplet<String, String, String>> deviceStringMap = queryDeviceStringsMap(devicesSeen);
        // recursively build results
        for (String controllerDeviceId : controllerDeviceIdList) {
            WindowsUsbDevice deviceAndChildren = getDeviceAndChildren(controllerDeviceId, "0000", "0000", deviceTreeMap,
                    deviceStringMap);
            if (deviceAndChildren != null) {
                controllerDevices.add(deviceAndChildren);
            }
        }
        return controllerDevices;
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
        for (UsbDevice device : list) {
            deviceList.add(new WindowsUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), device.getUniqueDeviceId(),
                    Collections.emptyList()));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static Map<String, Triplet<String, String, String>> queryDeviceStringsMap(Set<String> devicesToAdd) {
        Map<String, Triplet<String, String, String>> deviceStringCache = new HashMap<>();
        // Add devices not in the tree
        if (!devicesToAdd.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String deviceID : devicesToAdd) {
                if (first) {
                    sb.append(" WHERE (PnPDeviceID=\"");
                    first = false;
                } else {
                    sb.append(" OR (PnPDeviceID=\"");
                }
                sb.append(deviceID).append("\")");
            }
            String whereClause = sb.toString();
            // Get serial # for disk drives or other physical media
            Map<String, String> pnpToSerialMap = new HashMap<>();
            WmiResult<DeviceIdProperty> serialNumbers = Win32DiskDrive.queryDiskDriveId(whereClause);
            for (int i = 0; i < serialNumbers.getResultCount(); i++) {
                String pnpDeviceID = WmiUtil.getString(serialNumbers, DeviceIdProperty.PNPDEVICEID, i);
                if (deviceStringCache.containsKey(pnpDeviceID)) {
                    pnpToSerialMap.put(pnpDeviceID, ParseUtil
                            .hexStringToString(WmiUtil.getString(serialNumbers, DeviceIdProperty.SERIALNUMBER, i)));
                }
            }
            // Query Win32_PnPEntity to populate the maps
            WmiResult<PnPEntityProperty> pnpEntity = Win32PnPEntity.queryDeviceId(whereClause);
            for (int i = 0; i < pnpEntity.getResultCount(); i++) {
                String pnpDeviceID = WmiUtil.getString(pnpEntity, PnPEntityProperty.PNPDEVICEID, i);
                String name = WmiUtil.getString(pnpEntity, PnPEntityProperty.NAME, i);
                String vendor = WmiUtil.getString(pnpEntity, PnPEntityProperty.MANUFACTURER, i);
                deviceStringCache.put(pnpDeviceID,
                        new Triplet<>(name, vendor, pnpToSerialMap.getOrDefault(pnpDeviceID, "")));
                LOG.debug("Adding {} to USB device cache.", pnpDeviceID);
            }
        }
        return deviceStringCache;
    }

    /**
     * Navigates the Device Tree to place all children PNPDeviceIDs into the map for
     * the specified deviceID. Recursively adds children's children, etc.
     *
     * @param deviceId
     *            The device to add respective children to the map
     * @param deviceInstance
     *            The device instance (devnode handle), if known. If set to 0, the
     *            code will search for a match.
     * @param deviceTreeMap
     *            The overall device tree map that starts at the controllers
     * @param devicesSeen
     *            Devices we've seen so we can add/remove from cache later
     */
    private static void putChildrenInDeviceTree(String deviceId, int deviceInstance,
            Map<String, List<String>> deviceTreeMap, Set<String> devicesSeen) {
        devicesSeen.add(deviceId);
        // If no devInst provided, find it
        int devInst = deviceInstance;
        if (devInst == 0) {
            IntByReference pdnDevInst = new IntByReference();
            Cfgmgr32.INSTANCE.CM_Locate_DevNode(pdnDevInst, deviceId, 0);
            devInst = pdnDevInst.getValue();
        }
        // Now iterate the children. Call CM_Get_Child to get first child
        IntByReference child = new IntByReference();
        if (0 == Cfgmgr32.INSTANCE.CM_Get_Child(child, devInst, 0)) {
            // Add first child to a list
            List<String> childList = new ArrayList<>();
            String childId = Cfgmgr32Util.CM_Get_Device_ID(child.getValue());
            childList.add(childId);
            deviceTreeMap.put(deviceId, childList);
            putChildrenInDeviceTree(childId, child.getValue(), deviceTreeMap, devicesSeen);
            // Find any other children
            IntByReference sibling = new IntByReference();
            while (0 == Cfgmgr32.INSTANCE.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                // Add to the list
                String siblingId = Cfgmgr32Util.CM_Get_Device_ID(sibling.getValue());
                deviceTreeMap.get(deviceId).add(siblingId);
                putChildrenInDeviceTree(siblingId, sibling.getValue(), deviceTreeMap, devicesSeen);
                // Make this sibling the new child to find other siblings
                child = sibling;
            }
        }
    }

    private static WindowsUsbDevice getDeviceAndChildren(String hubDeviceId, String vid, String pid,
            Map<String, List<String>> deviceTreeMap, Map<String, Triplet<String, String, String>> deviceStringMap) {
        String vendorId = vid;
        String productId = pid;
        Pair<String, String> idPair = ParseUtil.parsePnPDeviceIdToVendorProductId(hubDeviceId);
        if (idPair != null) {
            vendorId = idPair.getA();
            productId = idPair.getB();
        }
        List<String> pnpDeviceIds = deviceTreeMap.getOrDefault(hubDeviceId, new ArrayList<>());
        List<UsbDevice> usbDevices = new ArrayList<>();
        for (String pnpDeviceId : pnpDeviceIds) {
            WindowsUsbDevice deviceAndChildren = getDeviceAndChildren(pnpDeviceId, vendorId, productId, deviceTreeMap,
                    deviceStringMap);
            if (deviceAndChildren != null) {
                usbDevices.add(deviceAndChildren);
            }
        }
        Collections.sort(usbDevices);
        if (deviceStringMap.containsKey(hubDeviceId)) {
            // name, vendor, serial
            Triplet<String, String, String> device = deviceStringMap.get(hubDeviceId);
            String name = device.getA();
            if (name.isEmpty()) {
                name = vendorId + ":" + productId;
            }
            return new WindowsUsbDevice(name, device.getB(), vendorId, productId, device.getC(), hubDeviceId,
                    usbDevices);
        }
        return null;
    }

    /**
     * Queries the USB Controller list
     *
     * @return A list of Strings of USB Controller PNPDeviceIDs
     */
    private static List<String> getControllerDeviceIdList() {
        List<String> controllerDeviceIdsList = new ArrayList<>();
        // One time lookup of USB Controller PnP Device IDs which don't
        // change
        WmiResult<USBControllerProperty> usbController = Win32USBController.queryUSBControllers();
        for (int i = 0; i < usbController.getResultCount(); i++) {
            controllerDeviceIdsList.add(WmiUtil.getString(usbController, USBControllerProperty.PNPDEVICEID, i));
        }
        return controllerDeviceIdsList;
    }
}
