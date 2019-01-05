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

import com.sun.jna.platform.win32.Cfgmgr32; // NOSONAR squid:S1191
import com.sun.jna.platform.win32.Cfgmgr32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.ptr.IntByReference;

import oshi.hardware.UsbDevice;
import oshi.hardware.platform.windows.WindowsUsbDevice.DiskDriveProperty;
import oshi.hardware.platform.windows.WindowsUsbDevice.PnPEntityProperty;
import oshi.util.MapUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

final class WindowsUsbDeviceCollector {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsUsbDeviceCollector.class);

    private static final Pattern VENDOR_PRODUCT_ID = Pattern
            .compile(".*(?:VID|VEN)_(\\p{XDigit}{4})&(?:PID|DEV)_(\\p{XDigit}{4}).*");

    private static final String PNPENTITY_BASE_CLASS = "Win32_PnPEntity";
    private static final String DISKDRIVE_BASE_CLASS = "Win32_DiskDrive";

    private final WindowsUsbDeviceCache cache;
    private final Map<String, WindowsUsbDevice> byPnpDeviceId = new HashMap<>();
    private Set<String> toAdd;
    private Set<String> toRemove;
    private Map<String, List<String>> treeMap;

    private WindowsUsbDeviceCollector(WindowsUsbDeviceCache cache) {
        this.cache = cache;
    }

    /**
     * @return An mutable list.
     */
    private List<UsbDevice> collectImpl(boolean tree) {
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

    private static void addDevicesToList(List<UsbDevice> deviceList, UsbDevice[] connectedDevices) {
        for (UsbDevice device : connectedDevices) {
            deviceList.add(new WindowsUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), new UsbDevice[0]));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    /**
     * @return An mutable list.
     */
    private List<UsbDevice> getUsbDevices() {
        // The controllerDeviceIDList contains the PnPDeviceIDs of the
        // controllers.

        // The deviceCache stores UsbDevices that we have already returned
        // Navigate the device tree to see what's present: remove from cache if
        // no longer present and add to cache if not present

        // Clear recursive ID map and set up sets
        treeMap = new HashMap<>();
        // Add any seen in tree that aren't in cache
        toAdd = new HashSet<>();
        // Also remove from cache if not seen in tree
        toRemove = new HashSet<>(byPnpDeviceId.keySet());

        List<String> pnpDeviceIds = cache.getPnpDeviceIds();
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controllerDeviceId : pnpDeviceIds) {
            putChildrenInDeviceTree(controllerDeviceId, 0);
            updateDeviceCache();
            WindowsUsbDevice deviceAndChildren = getDeviceAndChildren(controllerDeviceId, "0000", "0000");
            if (deviceAndChildren != null) {
                controllerDevices.add(deviceAndChildren);
            }
        }
        return controllerDevices;
    }

    private void updateDeviceCache() {

        // Remove devices no longer in tree
        for (String deviceID : toRemove) {
            byPnpDeviceId.remove(deviceID);
            LOG.debug("Removing {} from USB device cache.", deviceID);
        }
        toRemove.clear();
        // Create list to add
        if (!toAdd.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String deviceID : toAdd) {
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
            WmiQuery<PnPEntityProperty> pnpEntityQuery = new WmiQuery<>(null, PnPEntityProperty.class);
            pnpEntityQuery.setWmiClassName(PNPENTITY_BASE_CLASS + whereClause);
            WbemcliUtil.WmiResult<PnPEntityProperty> pnpEntity = WmiQueryHandler.getInstance().queryWMI(pnpEntityQuery);
            for (int i = 0; i < pnpEntity.getResultCount(); i++) {
                String pnpDeviceID = WmiUtil.getString(pnpEntity, PnPEntityProperty.PNPDEVICEID, i);
                String name = WmiUtil.getString(pnpEntity, PnPEntityProperty.NAME, i);
                String vendor = WmiUtil.getString(pnpEntity, PnPEntityProperty.MANUFACTURER, i);
                WindowsUsbDevice device = new WindowsUsbDevice(name, vendor, null, null, "", new WindowsUsbDevice[0]);
                byPnpDeviceId.put(pnpDeviceID, device);
                LOG.debug("Adding {} to USB device cache.", pnpDeviceID);
            }
            // Get serial # for disk drives or other physical media
            WmiQuery<DiskDriveProperty> diskDriveQuery = new WmiQuery<>(null, DiskDriveProperty.class);
            diskDriveQuery.setWmiClassName(DISKDRIVE_BASE_CLASS + whereClause);
            WbemcliUtil.WmiResult<DiskDriveProperty> serialNumber = WmiQueryHandler.getInstance()
                    .queryWMI(diskDriveQuery);
            for (int i = 0; i < serialNumber.getResultCount(); i++) {
                String pnpDeviceID = WmiUtil.getString(serialNumber, DiskDriveProperty.PNPDEVICEID, i);
                if (byPnpDeviceId.containsKey(pnpDeviceID)) {
                    WindowsUsbDevice device = byPnpDeviceId.get(pnpDeviceID);
                    device.setSerialNumber(ParseUtil
                            .hexStringToString(WmiUtil.getString(serialNumber, DiskDriveProperty.SERIALNUMBER, i)));
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
    private void putChildrenInDeviceTree(String deviceId, int deviceInstance) {
        // Track deviceIDs to add/remove from cache
        toRemove.remove(deviceId);
        if (!byPnpDeviceId.containsKey(deviceId)) {
            toAdd.add(deviceId);
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
            treeMap.put(deviceId, childList);
            putChildrenInDeviceTree(childId, child.getValue());
            // Find any other children
            IntByReference sibling = new IntByReference();
            while (0 == Cfgmgr32.INSTANCE.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                // Add to the list
                String siblingId = Cfgmgr32Util.CM_Get_Device_ID(sibling.getValue());
                treeMap.get(deviceId).add(siblingId);
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
     * @return A WindowsUsbDevice corresponding to this deviceID, or null if
     *         unable to find
     */
    private WindowsUsbDevice getDeviceAndChildren(String hubDeviceId, String vid, String pid) {
        String vendorId = vid;
        String productId = pid;
        Matcher m = VENDOR_PRODUCT_ID.matcher(hubDeviceId);
        if (m.matches()) {
            vendorId = m.group(1).toLowerCase();
            productId = m.group(2).toLowerCase();
        }
        List<String> pnpDeviceIds = MapUtil.getOrDefault(treeMap, hubDeviceId, new ArrayList<String>());
        List<WindowsUsbDevice> usbDevices = new ArrayList<>();
        for (String pnpDeviceId : pnpDeviceIds) {
            WindowsUsbDevice deviceAndChildren = getDeviceAndChildren(pnpDeviceId, vendorId, productId);
            if (deviceAndChildren != null) {
                usbDevices.add(deviceAndChildren);
            }
        }
        Collections.sort(usbDevices);
        if (byPnpDeviceId.containsKey(hubDeviceId)) {
            WindowsUsbDevice device = byPnpDeviceId.get(hubDeviceId);
            if (device.getName().isEmpty()) {
                device.setName(vendorId + ":" + productId);
            }
            device.setVendorId(vendorId);
            device.setProductId(productId);
            device.setConnectedDevices(usbDevices.toArray(new WindowsUsbDevice[0]));
            return device;
        }
        return null;
    }

    /**
     * @return An mutable list.
     */
    static List<UsbDevice> collect(WindowsUsbDeviceCache cache, boolean tree) {
        WindowsUsbDeviceCollector collector = new WindowsUsbDeviceCollector(cache);
        return collector.collectImpl(tree);
    }
}
