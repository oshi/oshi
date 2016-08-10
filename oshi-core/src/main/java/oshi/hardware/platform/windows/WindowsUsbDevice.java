/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.hardware.platform.mac.MacUsbDevice;
import oshi.jna.platform.windows.Cfgmgr32;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    private static final Logger LOG = LoggerFactory.getLogger(WindowsUsbDevice.class);

    private static final Pattern VENDOR_PRODUCT_ID = Pattern
            .compile(".*(?:VID|VEN)_(\\p{XDigit}{4})&(?:PID|DEV)_(\\p{XDigit}{4}).*");

    /*
     * Maps to store information using PNPDeviceID as the key
     */
    private static Map<String, String> nameMap = new HashMap<>();
    private static Map<String, String> vendorMap = new HashMap<>();
    private static Map<String, String> serialMap = new HashMap<>();
    private static Map<String, List<String>> hubMap = new HashMap<>();

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
                    device.getProductId(), device.getSerialNumber(), new MacUsbDevice[0]));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static UsbDevice[] getUsbDevices() {
        // Start by collecting information for all PNP devices. While in theory
        // these could be individually queried with a WHERE clause, grabbing
        // them all up front incurs minimal memory overhead in exchange for
        // faster access later

        // Clear maps
        nameMap.clear();
        vendorMap.clear();
        serialMap.clear();
        hubMap.clear();

        // Query Win32_PnPEntity to populate the maps
        Map<String, List<String>> usbMap = WmiUtil.selectStringsFrom(null, "Win32_PnPEntity",
                "Name,Manufacturer,PnPDeviceID", null);
        for (int i = 0; i < usbMap.get("Name").size(); i++) {
            String pnpDeviceID = usbMap.get("PnPDeviceID").get(i);
            nameMap.put(pnpDeviceID, usbMap.get("Name").get(i));
            if (usbMap.get("Manufacturer").get(i).length() > 0) {
                vendorMap.put(pnpDeviceID, usbMap.get("Manufacturer").get(i));
            }
        }

        // Get serial # for disk drives or other physical media
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_DiskDrive", "PNPDeviceID,SerialNumber", null);
        for (int i = 0; i < usbMap.get("PNPDeviceID").size(); i++) {
            serialMap.put(usbMap.get("PNPDeviceID").get(i),
                    ParseUtil.hexStringToString(usbMap.get("PNPDeviceID").get(i)));
        }
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_PhysicalMedia", "PNPDeviceID,SerialNumber", null);
        for (int i = 0; i < usbMap.get("PNPDeviceID").size(); i++) {
            serialMap.put(usbMap.get("PNPDeviceID").get(i),
                    ParseUtil.hexStringToString(usbMap.get("PNPDeviceID").get(i)));
        }

        // Build the device tree. Start with the USB Controllers
        // and recurse downward to devices as needed
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_USBController", "PNPDeviceID", null);
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controllerDeviceId : usbMap.get("PNPDeviceID")) {
            putChildrenInDeviceTree(controllerDeviceId, 0);
            controllerDevices.add(getDeviceAndChildren(controllerDeviceId, "0000", "0000"));
        }
        return controllerDevices.toArray(new UsbDevice[controllerDevices.size()]);
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
        int devInst = deviceInstance;
        // If no devInst provided, find it by matching deviceId
        if (devInst == 0) {
            // Get a handle to the device with this deviceId
            // Start with all classes
            HANDLE hinfoSet = SetupApi.INSTANCE.SetupDiGetClassDevs(null, null, null, SetupApi.DIGCF_ALLCLASSES);
            if (hinfoSet == WinNT.INVALID_HANDLE_VALUE) {
                LOG.error("Invalid handle value for {}. Error code: {}", deviceId, Native.getLastError());
                return;
            }
            // Iterate to find matching parent
            SP_DEVINFO_DATA dinfo = new SP_DEVINFO_DATA();
            dinfo.cbSize = dinfo.size();
            int i = 0;
            while (SetupApi.INSTANCE.SetupDiEnumDeviceInfo(hinfoSet, i++, dinfo)) {
                if (deviceId.equals(getDeviceId(dinfo.DevInst))) {
                    devInst = dinfo.DevInst;
                    break;
                }
            }
        }
        if (devInst == 0) {
            LOG.error("Unable to find a devnode handle for {}.", deviceId);
            return;
        }
        // Now iterate the children. Call CM_Get_Child to get first child
        IntByReference child = new IntByReference();
        if (0 == Cfgmgr32.INSTANCE.CM_Get_Child(child, devInst, 0)) {
            // Add first child to a list
            List<String> childList = new ArrayList<>();
            String childId = getDeviceId(child.getValue());
            childList.add(childId);
            hubMap.put(deviceId, childList);
            putChildrenInDeviceTree(childId, child.getValue());
            // Find any other children
            IntByReference sibling = new IntByReference();
            while (0 == Cfgmgr32.INSTANCE.CM_Get_Sibling(sibling, child.getValue(), 0)) {
                // Add to the list
                String siblingId = getDeviceId(sibling.getValue());
                hubMap.get(deviceId).add(siblingId);
                putChildrenInDeviceTree(siblingId, sibling.getValue());
                // Make this sibling the new child to find other siblings
                child = sibling;
            }
        }
    }

    /**
     * Gets the device id for a devnode
     *
     * @param devInst
     *            the handle to the devnode
     * @return The PNPDeviceID
     */
    private static String getDeviceId(int devInst) {
        NativeLongByReference ulLen = new NativeLongByReference();
        if (0 != Cfgmgr32.INSTANCE.CM_Get_Device_ID_Size(ulLen, devInst, 0)) {
            LOG.error("Couldn't get device string for device instance {}", devInst);
            return "";
        }
        // Add 1 for null terminator
        int size = ulLen.getValue().intValue() + 1;
        char[] buffer = new char[size];
        if (0 != Cfgmgr32.INSTANCE.CM_Get_Device_ID(devInst, buffer, size, 0)) {
            LOG.error("Couldn't get device string for device instance {} with size {}", devInst, size);
            return "";
        }
        return new String(buffer).trim();
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
        List<String> pnpDeviceIds = hubMap.getOrDefault(hubDeviceId, new ArrayList<String>());
        List<WindowsUsbDevice> usbDevices = new ArrayList<>();
        for (String pnpDeviceId : pnpDeviceIds) {
            usbDevices.add(getDeviceAndChildren(pnpDeviceId, vendorId, productId));
        }
        Collections.sort(usbDevices);
        return new WindowsUsbDevice(nameMap.getOrDefault(hubDeviceId, vendorId + ":" + productId),
                vendorMap.getOrDefault(hubDeviceId, ""), vendorId, productId, serialMap.getOrDefault(hubDeviceId, ""),
                usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }
}
