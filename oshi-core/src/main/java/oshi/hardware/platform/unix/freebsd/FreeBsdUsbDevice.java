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
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public class FreeBsdUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    public FreeBsdUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            UsbDevice[] connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, connectedDevices);
    }

    /*
     * Maps to store information using node # as the key
     */
    private static Map<String, String> nameMap = new HashMap<>();
    private static Map<String, String> vendorMap = new HashMap<>();
    private static Map<String, String> vendorIdMap = new HashMap<>();
    private static Map<String, String> productIdMap = new HashMap<>();
    private static Map<String, String> serialMap = new HashMap<>();
    private static Map<String, String> parentMap = new HashMap<>();
    private static Map<String, List<String>> hubMap = new HashMap<>();

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
            deviceList.add(new FreeBsdUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), new FreeBsdUsbDevice[0]));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList.toArray(new UsbDevice[deviceList.size()]);
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, UsbDevice[] connectedDevices) {
        for (UsbDevice device : connectedDevices) {
            deviceList.add(device);
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static UsbDevice[] getUsbDevices() {
        // Empty out maps
        // Empty out maps
        nameMap.clear();
        vendorMap.clear();
        vendorIdMap.clear();
        productIdMap.clear();
        serialMap.clear();
        hubMap.clear();
        parentMap.clear();

        // Enumerate all devices and build information maps. This will build the
        // entire device tree; we will identify the controllers as the parents
        // of the usbus entries and eventually only populate the returned
        // results with those
        List<String> devices = ExecutingCommand.runNative("lshal");
        if (devices.isEmpty()) {
            // TODO usbconfig, works as root
            return new FreeBsdUsbDevice[0];
        }
        // For each item enumerated, store information in the maps
        String key = "";
        List<String> usBuses = new ArrayList<String>();
        for (String line : devices) {
            // udi = ... identifies start of a new tree
            if (line.startsWith("udi =")) {
                // Remove indent for key
                key = getLshalString(line);
                continue;
            } else if (key.isEmpty()) {
                // Ignore everything preceding the first node
                continue;
            }
            // We are currently processing for node identified by key. Save
            // approrpriate variables to maps.
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            } else if (line.startsWith("freebsd.driver =") && getLshalString(line).equals("usbus")) {
                usBuses.add(key);
            } else if (line.contains(".parent =")) {
                String parent = getLshalString(line);
                // If this is interface of parent, skip
                if (key.replace(parent, "").startsWith("_if")) {
                    continue;
                }
                // Store parent for later usbus-skipping
                parentMap.put(key, parent);
                // Add this key to the parent's hubmap list
                hubMap.computeIfAbsent(parent, k -> new ArrayList<String>()).add(key);
            } else if (line.contains(".vendor =")) {
                vendorMap.put(key, getLshalString(line));
            } else if (line.contains(".product =")) {
                nameMap.put(key, getLshalString(line));
            } else if (line.contains(".serial =")) {
                String serial = getLshalString(line);
                serialMap.put(key,
                        serial.startsWith("0x") ? ParseUtil.hexStringToString(serial.replace("0x", "")) : serial);
            } else if (line.contains(".vendor_id =")) {
                vendorIdMap.put(key, getLshalIntAsHex(line));
            } else if (line.contains(".product_id =")) {
                vendorIdMap.put(key, getLshalIntAsHex(line));
            }
        }

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<UsbDevice>();
        for (String usbus : usBuses) {
            // Skip the usbuses: make their parents the controllers and replace
            // parents' children with the buses' children
            String parent = parentMap.get(usbus);
            hubMap.put(parent, hubMap.get(usbus));
            controllerDevices.add(getDeviceAndChildren(parent, "0000", "0000"));
        }
        return controllerDevices.toArray(new UsbDevice[controllerDevices.size()]);
    }

    /**
     * Parses a string key = 'value' (string)
     * 
     * @param line
     *            The entire string from lshal
     * @return the value contained between single tick marks
     */
    private static String getLshalString(String line) {
        String[] split = line.split("'");
        if (split.length < 2) {
            return "";
        }
        return split[1];
    }

    /**
     * Parses a string key = 128 (0x80) (int)
     * 
     * @param line
     *            The entire string from lshal
     * @return a 4 digit hex string representing the int (e.g., "0080")
     */
    private static String getLshalIntAsHex(String line) {
        String[] split = line.split("=|\\(");
        if (split.length < 2) {
            return "0000";
        }
        return String.format("%04x", ParseUtil.parseIntOrDefault(split[1].trim(), 0));
    }

    /**
     * Recursively creates SolarisUsbDevices by fetching information from maps
     * to populate fields
     * 
     * @param devPath
     *            The device node path.
     * @param vid
     *            The default (parent) vendor ID
     * @param pid
     *            The default (parent) product ID
     * @return A SolarisUsbDevice corresponding to this device
     */
    private static FreeBsdUsbDevice getDeviceAndChildren(String devPath, String vid, String pid) {
        String vendorId = vendorIdMap.getOrDefault(devPath, vid);
        String productId = productIdMap.getOrDefault(devPath, pid);
        List<String> childPaths = hubMap.getOrDefault(devPath, new ArrayList<String>());
        List<FreeBsdUsbDevice> usbDevices = new ArrayList<>();
        for (String path : childPaths) {
            usbDevices.add(getDeviceAndChildren(path, vid, pid));
        }
        Collections.sort(usbDevices);
        return new FreeBsdUsbDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId), "", vendorId, productId,
                "", usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }
}
