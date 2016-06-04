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
package oshi.hardware.platform.linux;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.FileUtil;

public class LinuxUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 1L;

    private static final String USB_ROOT = "/sys/bus/usb/devices/";

    public LinuxUsbDevice(String name, String vendor, String serialNumber, UsbDevice[] connectedDevices) {
        super(name, vendor, serialNumber, connectedDevices);
    }

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices() {
        // Structure of /sys/bus/usb/devices/
        // 1-0:1.0 <-- Bus interface. Ignore all :x.x
        // 1-1 <-- Bus 1, Port 1
        // 1-1.3 <-- Bus 1, Port 1, Port 3 of that
        // 1-1.3.1 <-- Bus 1, port 1, Port 3 of that, Port 1 of that
        // 1-1.3.1:1.0 <-- Device interface, ignore
        // 1-1.3:1.0
        // 1-1:1.0
        // usb1 <-- USB Controller, BUS #. Iterate over these at highest level
        // Each of the folders usb# and #-#.# will have /product, /manufacturer,
        // /serial. Do not read from interfaces.

        // Get buses, usb#
        File usbdir = new File(USB_ROOT);
        final Pattern p = Pattern.compile("usb\\d+");
        File[] buses = usbdir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return p.matcher(file.getName()).matches();
            }
        });
        if (buses == null) {
            return new UsbDevice[0];
        }

        List<UsbDevice> usbDevices = new ArrayList<UsbDevice>();
        for (File bus : buses) {
            // bus.toString() is path to info files and any nested devices
            usbDevices.add(getUsbDevice(bus.toString() + "/", ""));
        }
        return usbDevices.toArray(new UsbDevice[usbDevices.size()]);
    }

    /**
     * Get a USB controller/hub or device at a given path
     * 
     * @param path
     *            Path to the device
     * @return The corresponding USB device
     */
    private static UsbDevice getUsbDevice(String path, String parent) {
        String name = FileUtil.getStringFromFile(path + parent + "/product");
        String vendor = FileUtil.getStringFromFile(path + parent + "/manufacturer");
        String serialNumber = FileUtil.getStringFromFile(path + parent + "/serial");

        File usbdir = new File(path);
        final Pattern p = Pattern.compile(parent.length() > 0 ? parent + "\\.\\d+" : "\\d+-\\d+");
        File[] devices = usbdir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return p.matcher(file.getName()).matches();
            }
        });

        return new LinuxUsbDevice(name, vendor, serialNumber,
                devices == null ? new UsbDevice[0] : getUsbDevices(path, devices));
    }

    /**
     * Get all USB devices at the specified paths
     * 
     * @param devices
     *            an array of File objects with paths to devices
     * @return an array of corresponding USB devices
     */
    private static UsbDevice[] getUsbDevices(String path, File[] devices) {
        List<UsbDevice> usbDevices = new ArrayList<UsbDevice>();
        for (File device : devices) {
            // device.toString() is path to info files and any nested devices
            usbDevices.add(getUsbDevice(path, device.toString().replace(path, "")));
        }
        return usbDevices.toArray(new UsbDevice[usbDevices.size()]);
    }
}
