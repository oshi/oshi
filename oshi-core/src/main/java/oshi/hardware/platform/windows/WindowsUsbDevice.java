/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Cfgmgr32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Cfgmgr32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsUsbDevice.class);

    enum USBControllerProperty {
        PNPDEVICEID;
    }

    // List of controllers
    private static List<String> controllerDeviceIds = null;

    enum PnPEntityProperty {
        NAME, MANUFACTURER, PNPDEVICEID;
    }

    private static final String PNPENTITY_BASE_CLASS = "Win32_PnPEntity";
    private static final WmiQuery<PnPEntityProperty> PNPENTITY_QUERY = new WmiQuery<>(null, PnPEntityProperty.class);

    enum DiskDriveProperty {
        PNPDEVICEID, SERIALNUMBER;
    }

    private static final String DISKDRIVE_BASE_CLASS = "Win32_DiskDrive";
    private static final WmiQuery<DiskDriveProperty> DISKDRIVE_QUERY = new WmiQuery<>(null, DiskDriveProperty.class);

    private static final Pattern VENDOR_PRODUCT_ID = Pattern
            .compile(".*(?:VID|VEN)_(\\p{XDigit}{4})&(?:PID|DEV)_(\\p{XDigit}{4}).*");

    // Map to store information using PNPDeviceID as the key
    private static Map<String, WindowsUsbDevice> usbDeviceCache = new HashMap<>();

    public WindowsUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            UsbDevice[] connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, connectedDevices);
    }

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices(boolean tree) {
        UsbDevice[] devices = getUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        // Top level is controllers; they won't be added to the list, but all
        // their connected devices will be
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList.toArray(new UsbDevice[deviceList.size()]);
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, UsbDevice[] connectedDevices) {
        for (UsbDevice device : connectedDevices) {
            deviceList.add(new WindowsUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), new UsbDevice[0]));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static UsbDevice[] getUsbDevices() {
        // Map to build the recursive tree structure
        Map<String, List<String>> deviceTreeMap = new HashMap<>();
        // Track devices seen in the process
        Set<String> devicesSeen = new HashSet<>();

        // Navigate the device tree to track what devices are present
        List<WindowsUsbDevice> controllerDevices = new ArrayList<>();
        WmiQueryHandler wmiQueryHandler = WmiQueryHandler.createInstance();
        List<String> controllerDeviceIdList = getControllerDeviceIdList(wmiQueryHandler);
        for (String controllerDeviceId : controllerDeviceIdList) {
            putChildrenInDeviceTree(controllerDeviceId, 0, deviceTreeMap, devicesSeen);
        }
        // remove from cache if no longer present and add to cache if present
        // but not in cache
        updateDeviceCache(devicesSeen, wmiQueryHandler);
        // recursively build results
        for (String controllerDeviceId : controllerDeviceIdList) {
            WindowsUsbDevice deviceAndChildren = getDeviceAndChildren(controllerDeviceId, "0000", "0000",
                    deviceTreeMap);
            if (deviceAndChildren != null) {
                controllerDevices.add(deviceAndChildren);
            }
        }
        return controllerDevices.toArray(new WindowsUsbDevice[controllerDevices.size()]);
    }

    private static void updateDeviceCache(Set<String> devicesSeen, WmiQueryHandler wmiQueryHandler) {
        // The deviceCache stores UsbDevices that we have already returned
        // Remove devices no longer in tree
        Set<String> devicesToRemove = new HashSet<>(usbDeviceCache.keySet());
        devicesToRemove.removeAll(devicesSeen);
        for (String deviceID : devicesToRemove) {
            usbDeviceCache.remove(deviceID);
            LOG.debug("Removing {} from USB device cache.", deviceID);
        }

        // Add devices not in the tree
        Set<String> devicesToAdd = new HashSet<>(devicesSeen);
        devicesToAdd.removeAll(usbDeviceCache.keySet());
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
            // Query Win32_PnPEntity to populate the maps
            PNPENTITY_QUERY.setWmiClassName(PNPENTITY_BASE_CLASS + whereClause);
            WmiResult<PnPEntityProperty> pnpEntity = wmiQueryHandler.queryWMI(PNPENTITY_QUERY);
            for (int i = 0; i < pnpEntity.getResultCount(); i++) {
                String pnpDeviceID = WmiUtil.getString(pnpEntity, PnPEntityProperty.PNPDEVICEID, i);
                String name = WmiUtil.getString(pnpEntity, PnPEntityProperty.NAME, i);
                String vendor = WmiUtil.getString(pnpEntity, PnPEntityProperty.MANUFACTURER, i);
                WindowsUsbDevice device = new WindowsUsbDevice(name, vendor, null, null, "", new WindowsUsbDevice[0]);
                usbDeviceCache.put(pnpDeviceID, device);
                LOG.debug("Adding {} to USB device cache.", pnpDeviceID);
            }
            // Get serial # for disk drives or other physical media
            DISKDRIVE_QUERY.setWmiClassName(DISKDRIVE_BASE_CLASS + whereClause);
            WmiResult<DiskDriveProperty> serialNumber = wmiQueryHandler.queryWMI(DISKDRIVE_QUERY);
            for (int i = 0; i < serialNumber.getResultCount(); i++) {
                String pnpDeviceID = WmiUtil.getString(serialNumber, DiskDriveProperty.PNPDEVICEID, i);
                if (usbDeviceCache.containsKey(pnpDeviceID)) {
                    WindowsUsbDevice device = usbDeviceCache.get(pnpDeviceID);
                    device.serialNumber = ParseUtil
                            .hexStringToString(WmiUtil.getString(serialNumber, DiskDriveProperty.SERIALNUMBER, i));
                }
            }
        }
    }

    /**
     * Navigates the Device Tree to place all children PNPDeviceIDs into the map
     * for the specified deviceID. Recursively adds children's children, etc.
     * 
     * @param deviceId
     *            The device to add respective children to the map
     * @param deviceInstance
     *            The device instance (devnode handle), if known. If set to 0,
     *            the code will search for a match.
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

    /**
     * Recursively creates WindowsUsbDevices by fetching information from maps
     * to populate fields
     *
     * @param hubDeviceId
     *            The PNPdeviceID of this device.
     * @param vid
     *            The default (parent) vendor ID
     * @param pid
     *            The default (parent) product ID
     * @param deviceTreeMap
     * @return A WindowsUsbDevice corresponding to this deviceID, or null if
     *         unable to find
     */
    private static WindowsUsbDevice getDeviceAndChildren(String hubDeviceId, String vid, String pid,
            Map<String, List<String>> deviceTreeMap) {
        String vendorId = vid;
        String productId = pid;
        Matcher m = VENDOR_PRODUCT_ID.matcher(hubDeviceId);
        if (m.matches()) {
            vendorId = m.group(1).toLowerCase();
            productId = m.group(2).toLowerCase();
        }
        List<String> pnpDeviceIds = deviceTreeMap.getOrDefault(hubDeviceId, new ArrayList<String>());
        List<WindowsUsbDevice> usbDevices = new ArrayList<>();
        for (String pnpDeviceId : pnpDeviceIds) {
            WindowsUsbDevice deviceAndChildren = getDeviceAndChildren(pnpDeviceId, vendorId, productId, deviceTreeMap);
            if (deviceAndChildren != null) {
                usbDevices.add(deviceAndChildren);
            }
        }
        Collections.sort(usbDevices);
        if (usbDeviceCache.containsKey(hubDeviceId)) {
            WindowsUsbDevice device = usbDeviceCache.get(hubDeviceId);
            if (device.name.isEmpty()) {
                device.name = vendorId + ":" + productId;
            }
            device.vendorId = vendorId;
            device.productId = productId;
            device.connectedDevices = usbDevices.toArray(new WindowsUsbDevice[usbDevices.size()]);
            return device;
        }
        return null;
    }

    /**
     * Queries the USB Controller list, which doesn't change so we only need to
     * query it once
     * 
     * @param wmiQueryHandler
     * 
     * @return A list of Strings of USB Controller PNPDeviceIDs
     */
    private static List<String> getControllerDeviceIdList(WmiQueryHandler wmiQueryHandler) {
        if (controllerDeviceIds == null) {
            controllerDeviceIds = new ArrayList<>();
            // One time lookup of USB Controller PnP Device IDs which don't
            // change
            WmiQuery<USBControllerProperty> usbControllerQuery = new WmiQuery<>("Win32_USBController",
                    USBControllerProperty.class);
            WmiResult<USBControllerProperty> usbController = wmiQueryHandler.queryWMI(usbControllerQuery);
            for (int i = 0; i < usbController.getResultCount(); i++) {
                controllerDeviceIds.add(WmiUtil.getString(usbController, USBControllerProperty.PNPDEVICEID, i));
            }
        }
        return controllerDeviceIds;
    }
}