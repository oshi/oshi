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
import java.util.stream.Collectors;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ParseUtil;
import oshi.util.platform.windows.WmiUtil;

public class WindowsUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 1L;

    public WindowsUsbDevice(String name, String vendor, String serialNumber, UsbDevice[] connectedDevices) {
        super(name, vendor, serialNumber, connectedDevices);
    }

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices() {
        List<UsbDevice> usbDevices = new ArrayList<UsbDevice>();

        // Store map of pnpID to name, Manufacturer, serial
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();

        Map<String, List<String>> usbMap = WmiUtil.selectStringsFrom(null, "Win32_PnPEntity",
                "Name,Manufacturer,PnPDeviceID", null);
        for (int i = 0; i < usbMap.get("Name").size(); i++) {
            String pnpDeviceID = usbMap.get("PnPDeviceID").get(i);
            nameMap.put(pnpDeviceID, usbMap.get("Name").get(i));
            if (usbMap.get("Manufacturer").get(i).length() > 0) {
                vendorMap.put(pnpDeviceID, usbMap.get("Manufacturer").get(i));
            }
            String serialNumber = "";
            // Format: USB\VID_203A&PID_FFF9&MI_00\6&18C4CF61&0&0000
            // Split by \ to get bus type (USB), VendorID/ProductID/Model
            // Last field contains Serial # in hex as 2nd split by
            // ampersands
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

        // Get serial # of disk drives (may overwrite previous, that's OK)
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

        // Finally, prepare final list for output
        // Start with controllers
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_USBController", "PNPDeviceID", null);
        List<String> pnpDeviceIDs = usbMap.get("PNPDeviceID");
        // Add any hubs
        usbMap = WmiUtil.selectStringsFrom(null, "Win32_USBHub", "PNPDeviceID", null);
        pnpDeviceIDs.addAll(usbMap.get("PNPDeviceID"));
        // Add any stray USB devices in the list
        for (String pnpDeviceID : nameMap.keySet().stream().sorted().collect(Collectors.toList())) {
            if (pnpDeviceID.startsWith("USB\\") && !pnpDeviceIDs.contains(pnpDeviceID)) {
                pnpDeviceIDs.add(pnpDeviceID);
            }
        }

        for (String pnpDeviceID : pnpDeviceIDs) {
            usbDevices.add(
                    new WindowsUsbDevice(nameMap.getOrDefault(pnpDeviceID, ""), vendorMap.getOrDefault(pnpDeviceID, ""),
                            serialMap.getOrDefault(pnpDeviceID, ""), new WindowsUsbDevice[0]));
        }

        return usbDevices.toArray(new UsbDevice[usbDevices.size()]);
    }
}
