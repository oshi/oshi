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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
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

        Map<String, List<String>> usbMap = WmiUtil.selectStringsFrom(null, "Win32_PnPEntity",
                "Name,Manufacturer,PNPDeviceID", "WHERE PNPDeviceID Like \"USB\\\\%\" OR Name Like \"% USB%\"");
        for (int i = 0; i < usbMap.get("Name").size(); i++) {
            String name = usbMap.get("Name").get(i);
            String vendor = usbMap.get("Manufacturer").get(i);
            String serialNumber = "";
            // Format: USB\VID_203A&PID_FFF9&MI_00\6&18C4CF61&0&0000
            // Split by \ to get bus type (USB), VendorID/ProductID/Model
            // Last field contains Serial # in hex as 2nd split by ampersands
            String[] idSplit = usbMap.get("PNPDeviceID").get(i).split("\\\\");
            if (idSplit.length > 2) {
                idSplit = idSplit[2].split("&");
                if (idSplit.length > 3) {
                    serialNumber = idSplit[1];
                }
            }
            usbDevices.add(new WindowsUsbDevice(name, vendor, serialNumber, new UsbDevice[0]));
        }
        // WMI Order is somewhat random. Sorting results alphabetically by name
        usbDevices = usbDevices.stream().sorted((u1, u2) -> u1.getName().compareTo(u2.getName()))
                .collect(Collectors.toList());

        return usbDevices.toArray(new UsbDevice[usbDevices.size()]);
    }

}
