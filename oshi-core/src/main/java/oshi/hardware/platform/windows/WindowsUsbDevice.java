/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
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

import com.sun.jna.ptr.IntByReference;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.jna.platform.windows.Cfgmgr32;
import oshi.jna.platform.windows.Cfgmgr32Util;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.platform.windows.WmiUtil.ValueType;
import oshi.util.platform.windows.WmiUtil.WmiProperty;
import oshi.util.platform.windows.WmiUtil.WmiQuery;
import oshi.util.platform.windows.WmiUtil.WmiResult;

public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsUsbDevice.class);

    enum USBControllerProperty implements WmiProperty {
        PNPDEVICEID(ValueType.STRING);

        private ValueType type;

        USBControllerProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static List<String> controllerDeviceIdList = new ArrayList<>();
    static {
        // One time lookup of USB Controller PnP Device IDs which don't change
        WmiQuery<USBControllerProperty> usbControllerQuery = WmiUtil.createQuery("Win32_USBController",
                USBControllerProperty.class);
        WmiResult<USBControllerProperty> usbController = WmiUtil.queryWMI(usbControllerQuery);
        for (int i = 0; i < usbController.getResultCount(); i++) {
            controllerDeviceIdList.add((String) usbController.get(USBControllerProperty.PNPDEVICEID).get(i));
        }
    }

    enum PnPEntityProperty implements WmiProperty {
        NAME(ValueType.STRING), //
        MANUFACTURER(ValueType.STRING), //
        PNPDEVICEID(ValueType.STRING);

        private ValueType type;

        PnPEntityProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final String PNPENTITY_BASE_CLASS = "Win32_PnPEntity";
    private static final WmiQuery<PnPEntityProperty> PNPENTITY_QUERY = WmiUtil.createQuery(null,
            PnPEntityProperty.class);

    enum DiskDriveProperty implements WmiProperty {
        PNPDEVICEID(ValueType.STRING), //
        SERIALNUMBER(ValueType.STRING);

        private ValueType type;

        DiskDriveProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final String DISKDRIVE_BASE_CLASS = "Win32_DiskDrive";
    private static final WmiQuery<DiskDriveProperty> DISKDRIVE_QUERY = WmiUtil.createQuery(null,
            DiskDriveProperty.class);
    private static final String PHYSICALMEDIA_BASE_CLASS = "Win32_PhysicalMedia";
    private static final WmiQuery<DiskDriveProperty> PHYSICALMEDIA_QUERY = WmiUtil.createQuery(null,
            DiskDriveProperty.class);

    private static final Pattern VENDOR_PRODUCT_ID = Pattern
            .compile(".*(?:VID|VEN)_(\\p{XDigit}{4})&(?:PID|DEV)_(\\p{XDigit}{4}).*");

    /*
     * Map to store information using PNPDeviceID as the key
     */
    private static Map<String, WindowsUsbDevice> usbDeviceCache = new HashMap<>();

    /*
     * Sets to track USB devices in cache
     */
    private static Set<String> devicesToAdd = new HashSet<>();
    private static Set<String> devicesToRemove = new HashSet<>();

    /*
     * Map to build the recursive tree structure
     */
    private static Map<String, List<String>> deviceTreeMap = new HashMap<>();

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
        // The controllerDeviceIDList contains the PnPDeviceIDs of the
        // controllers.

        // The deviceCache stores UsbDevices that we have already returned
        // Navigate the device tree to see what's present: remove from cache if
        // no longer present and add to cache if not present

        // Clear recursive ID map and set up sets
        deviceTreeMap.clear();
        // Add any seen in tree that aren't in cache
        devicesToAdd.clear();
        // Also remove from cache if not seen in tree
        devicesToRemove = new HashSet<String>(usbDeviceCache.keySet());

        List<WindowsUsbDevice> controllerDevices = new ArrayList<>();
        for (String controllerDeviceId : controllerDeviceIdList) {
            putChildrenInDeviceTree(controllerDeviceId, 0);
            updateDeviceCache();
            controllerDevices.add(getDeviceAndChildren(controllerDeviceId, "0000", "0000"));
        }
        return controllerDevices.toArray(new WindowsUsbDevice[controllerDevices.size()]);

    }

    private static void updateDeviceCache() {

        // Remove devices no longer in tree
        for (String deviceID : devicesToRemove) {
            usbDeviceCache.remove(deviceID);
            LOG.debug("Removing {} from USB device cache.", deviceID);
        }
        devicesToRemove.clear();
        // Create list to add
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
            WmiResult<PnPEntityProperty> pnpEntity = WmiUtil.queryWMI(PNPENTITY_QUERY);
            for (int i = 0; i < pnpEntity.getResultCount(); i++) {
                String pnpDeviceID = (String) pnpEntity.get(PnPEntityProperty.PNPDEVICEID).get(i);
                String name = (String) pnpEntity.get(PnPEntityProperty.NAME).get(i);
                String vendor = (String) pnpEntity.get(PnPEntityProperty.MANUFACTURER).get(i);
                WindowsUsbDevice device = new WindowsUsbDevice(name, vendor, null, null, "", new WindowsUsbDevice[0]);
                usbDeviceCache.put(pnpDeviceID, device);
                LOG.debug("Adding {} to USB device cache.", pnpDeviceID);
            }
            // Get serial # for disk drives or other physical media
            DISKDRIVE_QUERY.setWmiClassName(DISKDRIVE_BASE_CLASS + whereClause);
            WmiResult<DiskDriveProperty> serialNumber = WmiUtil.queryWMI(DISKDRIVE_QUERY);
            for (int i = 0; i < serialNumber.getResultCount(); i++) {
                String pnpDeviceID = (String) serialNumber.get(DiskDriveProperty.PNPDEVICEID).get(i);
                if (usbDeviceCache.containsKey(pnpDeviceID)) {
                    WindowsUsbDevice device = usbDeviceCache.get(pnpDeviceID);
                    device.serialNumber = ParseUtil
                            .hexStringToString((String) serialNumber.get(DiskDriveProperty.SERIALNUMBER).get(i));
                }
            }
            PHYSICALMEDIA_QUERY.setWmiClassName(PHYSICALMEDIA_BASE_CLASS + whereClause);
            serialNumber = WmiUtil.queryWMI(PHYSICALMEDIA_QUERY);
            for (int i = 0; i < serialNumber.getResultCount(); i++) {
                String pnpDeviceID = (String) serialNumber.get(DiskDriveProperty.PNPDEVICEID).get(i);
                if (usbDeviceCache.containsKey(pnpDeviceID)) {
                    WindowsUsbDevice device = usbDeviceCache.get(pnpDeviceID);
                    device.serialNumber = ParseUtil
                            .hexStringToString((String) serialNumber.get(DiskDriveProperty.SERIALNUMBER).get(i));
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
     */
    private static void putChildrenInDeviceTree(String deviceId, int deviceInstance) {
        // Track deviceIDs to add/remove from cache
        devicesToRemove.remove(deviceId);
        if (!usbDeviceCache.containsKey(deviceId)) {
            devicesToAdd.add(deviceId);
        }
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
            putChildrenInDeviceTree(childId, child.getValue());
            // Find any other children
            IntByReference sibling = new IntByReference();
            while (0 == Cfgmgr32.INSTANCE.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                // Add to the list
                String siblingId = Cfgmgr32Util.CM_Get_Device_ID(sibling.getValue());
                deviceTreeMap.get(deviceId).add(siblingId);
                putChildrenInDeviceTree(siblingId, sibling.getValue());
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
     * @return A WindowsUsbDevice corresponding to this deviceID
     */
    private static WindowsUsbDevice getDeviceAndChildren(String hubDeviceId, String vid, String pid) {
        String vendorId = vid;
        String productId = pid;
        Matcher m = VENDOR_PRODUCT_ID.matcher(hubDeviceId);
        if (m.matches()) {
            vendorId = m.group(1).toLowerCase();
            productId = m.group(2).toLowerCase();
        }
        List<String> pnpDeviceIds = MapUtil.getOrDefault(deviceTreeMap, hubDeviceId, new ArrayList<String>());
        List<WindowsUsbDevice> usbDevices = new ArrayList<>();
        for (String pnpDeviceId : pnpDeviceIds) {
            usbDevices.add(getDeviceAndChildren(pnpDeviceId, vendorId, productId));
        }
        Collections.sort(usbDevices);
        WindowsUsbDevice device = usbDeviceCache.get(hubDeviceId);
        if (device.name.isEmpty()) {
            device.name = vendorId + ":" + productId;
        }
        device.vendorId = vendorId;
        device.productId = productId;
        device.connectedDevices = usbDevices.toArray(new WindowsUsbDevice[usbDevices.size()]);
        return device;
    }
}
