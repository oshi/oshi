/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static java.util.Collections.emptyList;
import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.linux.LinuxUsbDevice;

/**
 * Linux USB device helper using JNA/udev. Instantiates {@link LinuxUsbDevice} objects.
 */
public final class LinuxUsbDeviceJNA extends LinuxUsbDevice {

    private LinuxUsbDeviceJNA() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinuxUsbDeviceJNA.class);

    private static final String SUBSYSTEM_USB = "usb";
    private static final String DEVTYPE_USB_DEVICE = "usb_device";
    private static final String ATTR_PRODUCT = "product";
    private static final String ATTR_MANUFACTURER = "manufacturer";
    private static final String ATTR_VENDOR_ID = "idVendor";
    private static final String ATTR_PRODUCT_ID = "idProduct";
    private static final String ATTR_SERIAL = "serial";

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @return a list of USB controllers, each with its connected-device tree.
     */
    public static List<UsbDevice> getUsbDevices() {
        return queryUsbDevices();
    }

    private static List<UsbDevice> queryUsbDevices() {
        if (!HAS_UDEV) {
            LOG.warn("USB Device information requires libudev, which is not present.");
            return emptyList();
        }
        // Build a list of devices with no parent; these will be the roots
        List<String> usbControllers = new ArrayList<>();

        // Maps to store information using device syspath as the key
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        // Enumerate all usb devices and build information maps
        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            LOG.warn("Failed to create udev context.");
            return emptyList();
        }
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem(SUBSYSTEM_USB);
                enumerate.scanDevices();

                // For each item enumerated, store information in the maps
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            // Only include usb_device devtype, skipping usb_interface
                            if (DEVTYPE_USB_DEVICE.equals(device.getDevtype())) {
                                String value = device.getSysattrValue(ATTR_PRODUCT);
                                if (value != null) {
                                    nameMap.put(syspath, value);
                                }
                                value = device.getSysattrValue(ATTR_MANUFACTURER);
                                if (value != null) {
                                    vendorMap.put(syspath, value);
                                }
                                value = device.getSysattrValue(ATTR_VENDOR_ID);
                                if (value != null) {
                                    vendorIdMap.put(syspath, value);
                                }
                                value = device.getSysattrValue(ATTR_PRODUCT_ID);
                                if (value != null) {
                                    productIdMap.put(syspath, value);
                                }
                                value = device.getSysattrValue(ATTR_SERIAL);
                                if (value != null) {
                                    serialMap.put(syspath, value);
                                }

                                UdevDevice parent = device.getParentWithSubsystemDevtype(SUBSYSTEM_USB,
                                        DEVTYPE_USB_DEVICE);
                                if (parent == null) {
                                    // This is a controller with no parent, add to list
                                    usbControllers.add(syspath);
                                } else {
                                    // Add child syspath to parent's path
                                    String parentPath = parent.getSyspath();
                                    hubMap.computeIfAbsent(parentPath, x -> new ArrayList<>()).add(syspath);
                                }
                            }
                        } finally {
                            device.unref();
                        }
                    }
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }
}
