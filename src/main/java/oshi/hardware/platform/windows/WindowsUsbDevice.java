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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 1L;

    public WindowsUsbDevice(String name, String vendor, String serialNumber, UsbDevice[] connectedDevices) {
        super(name, vendor, serialNumber, connectedDevices);
    }

    /*
     * Maps to store information using PNPDeviceID as the key
     */
    private static Map<String, String> nameMap = new HashMap<>();
    private static Map<String, String> vendorMap = new HashMap<>();
    private static Map<String, String> serialMap = new HashMap<>();
    private static Map<String, List<String>> hubMap = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices() {
        // Start by collecting information for all PNP devices. While in theory
        // these could be individually queried with a WHERE clause, grabbing
        // them all up front incurs minimal memory overhead in exchange for
        // faster access later

        // Clear maps
        nameMap.clear();
        vendorMap.clear();
        serialMap.clear();

        // Query Win32_PnPEntity to populate the maps
        Map<String, List<String>> usbMap = WmiUtil.selectStringsFrom(null, "Win32_PnPEntity",
                "Name,Manufacturer,PnPDeviceID", null);
        for (int i = 0; i < usbMap.get("Name").size(); i++) {
            String pnpDeviceID = usbMap.get("PnPDeviceID").get(i);
            nameMap.put(pnpDeviceID, usbMap.get("Name").get(i));
            if (usbMap.get("Manufacturer").get(i).length() > 0) {
                vendorMap.put(pnpDeviceID, usbMap.get("Manufacturer").get(i));
            }
            String serialNumber = "";
            // PNPDeviceID: USB\VID_203A&PID_FFF9&MI_00\6&18C4CF61&0&0000
            // Split by \ to get bus type (USB), VendorID/ProductID, other info
            // As a temporary hack for a serial number, use last \-split field
            // using 2nd &-split field if 4 fields
            String[] idSplit = pnpDeviceID.split("\\\\");
            if (idSplit.length > 2) {
                idSplit = idSplit[2].split("&");
                if (idSplit.length > 3) {
                    serialNumber = idSplit[1];
                }
            }
            if (serialNumber.length() > 0) {
                serialMap.put(pnpDeviceID, serialNumber);
            }
        }

        // Disk drives or other physical media have a better way of getting
        // serial number. Grab these and overwrite the temporary serial number
        // assigned above if necessary
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

        // Some USB Devices are hubs to which other devices connect. Knowing
        // which ones are hubs will help later when walking the device tree
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_USBHub", "PNPDeviceID", null);
        List<String> usbHubs = usbMap.get("PNPDeviceID");

        // Now build the hub map linking USB devices with their parent hub.
        // At the top of the device tree are USB Controllers. All USB hubs and
        // devices descend from these. Because this query returns pointers it's
        // just not practical to try to query via COM so we use a command line
        // in order to get USB devices in a text format
        ArrayList<String> links = ExecutingCommand
                .runNative("wmic path Win32_USBControllerDevice GET Antecedent,Dependent");
        // This iteration actually walks the device tree in order so while the
        // antecedent of all USB devices is its controller, we know that if a
        // device is not a hub that the last hub listed is its parent
        // Devices with PNPDeviceID containing "ROOTHUB" are special and will be
        // parents of the next item(s)
        // This won't id chained hubs (other than the root hub) but is a quick
        // hack rather than walking the entire device tree using the SetupDI API
        // and good enough since exactly how a USB device is connected is
        // theoretically transparent to the user
        hubMap.clear();
        String currentHub = null;
        String rootHub = null;
        for (String s : links) {
            String[] split = s.split("\\s+");
            if (split.length < 2) {
                continue;
            }
            String antecedent = getId(split[0]);
            String dependent = getId(split[1]);
            // Ensure initial defaults are sane if something goes wrong
            if (currentHub == null || rootHub == null) {
                currentHub = antecedent;
                rootHub = antecedent;
            }
            String parent;
            if (dependent.contains("ROOT_HUB")) {
                // This is a root hub, assign controller as parent;
                parent = antecedent;
                rootHub = dependent;
                currentHub = dependent;
            } else if (usbHubs.contains(dependent)) {
                // This is a hub, assign parent as root hub
                if (rootHub == null) {
                    rootHub = antecedent;
                }
                parent = rootHub;
                currentHub = dependent;
            } else {
                // This is not a hub, assign parent as previous hub
                if (currentHub == null) {
                    currentHub = antecedent;
                }
                parent = currentHub;
            }
            // Finally add the parent/child linkage to the map
            if (!hubMap.containsKey(parent)) {
                hubMap.put(parent, new ArrayList<String>());
            }
            hubMap.get(parent).add(dependent);
        }

        // Finally we simply get the device IDs of the USB Controllers. These
        // will recurse downward to devices as needed
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_USBController", "PNPDeviceID", null);
        List<UsbDevice> controllerDevices = new ArrayList<UsbDevice>();
        for (String controllerDeviceID : usbMap.get("PNPDeviceID")) {
            controllerDevices.add(getDeviceAndChildren(controllerDeviceID));
        }
        return controllerDevices.toArray(new UsbDevice[controllerDevices.size()]);
    }

    /**
     * Recursively creates WindowsUsbDevices by fetching information from maps
     * to populate fields
     * 
     * @param hubDeviceID
     *            The PNPdeviceID of this device.
     * @return A WindowsUsbDevice corresponding to this deviceID
     */
    private static WindowsUsbDevice getDeviceAndChildren(String hubDeviceID) {
        List<String> pnpDeviceIDs = hubMap.getOrDefault(hubDeviceID, new ArrayList<>());
        List<WindowsUsbDevice> usbDevices = new ArrayList<>();
        for (String pnpDeviceID : pnpDeviceIDs) {
            usbDevices.add(getDeviceAndChildren(pnpDeviceID));
        }
        return new WindowsUsbDevice(nameMap.getOrDefault(hubDeviceID, ""), vendorMap.getOrDefault(hubDeviceID, ""),
                serialMap.getOrDefault(hubDeviceID, ""), usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }

    /**
     * Parses DeviceID from CIM_USBController text
     * 
     * @param string
     *            Text of form (stuff)DeviceID="(ID)"
     * @return The parsed device ID
     */
    private static String getId(String s) {
        String[] split = s.split("\"");
        return split.length < 2 ? "" : split[1];
    }
}
