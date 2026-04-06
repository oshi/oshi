/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static java.util.Collections.emptyList;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.linux.UdevFunctions;
import oshi.hardware.UsbDevice;

/**
 * Linux USB device helper using FFM/udev. Instantiates {@link LinuxUsbDevice} objects.
 */
public final class LinuxUsbDeviceFFM extends LinuxUsbDevice {

    private LinuxUsbDeviceFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(LinuxUsbDeviceFFM.class);

    private static final String SUBSYSTEM_USB = "usb";
    private static final String DEVTYPE_USB_DEVICE = "usb_device";
    private static final String ATTR_PRODUCT = "product";
    private static final String ATTR_MANUFACTURER = "manufacturer";
    private static final String ATTR_VENDOR_ID = "idVendor";
    private static final String ATTR_PRODUCT_ID = "idProduct";
    private static final String ATTR_SERIAL = "serial";

    /**
     * Instantiates a list of {@link oshi.hardware.UsbDevice} objects, representing devices connected via a usb port
     * (including internal devices).
     *
     * @param tree If true, returns a list of controllers with their device tree. If false, returns a flat list of
     *             devices excluding controllers.
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = queryUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static List<UsbDevice> queryUsbDevices() {
        if (!HAS_UDEV) {
            LOG.warn("USB Device information requires libudev, which is not present.");
            return emptyList();
        }
        List<String> usbControllers = new ArrayList<>();
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return emptyList();
            }
            try {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                try {
                    UdevFunctions.addMatchSubsystem(enumerate, SUBSYSTEM_USB, arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);

                    for (MemorySegment entry = UdevFunctions
                            .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                    .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                        MemorySegment namePtr = UdevFunctions.udev_list_entry_get_name(entry);
                        String syspath = UdevFunctions.getString(namePtr, arena);
                        if (syspath == null) {
                            continue;
                        }
                        MemorySegment device = UdevFunctions.deviceNewFromSyspath(udev, syspath, arena);
                        if (MemorySegment.NULL.equals(device)) {
                            continue;
                        }
                        try {
                            String devtype = UdevFunctions.getString(UdevFunctions.udev_device_get_devtype(device),
                                    arena);
                            if (!DEVTYPE_USB_DEVICE.equals(devtype)) {
                                continue;
                            }
                            String value = UdevFunctions.getSysattrValue(device, ATTR_PRODUCT, arena);
                            if (value != null) {
                                nameMap.put(syspath, value);
                            }
                            value = UdevFunctions.getSysattrValue(device, ATTR_MANUFACTURER, arena);
                            if (value != null) {
                                vendorMap.put(syspath, value);
                            }
                            value = UdevFunctions.getSysattrValue(device, ATTR_VENDOR_ID, arena);
                            if (value != null) {
                                vendorIdMap.put(syspath, value);
                            }
                            value = UdevFunctions.getSysattrValue(device, ATTR_PRODUCT_ID, arena);
                            if (value != null) {
                                productIdMap.put(syspath, value);
                            }
                            value = UdevFunctions.getSysattrValue(device, ATTR_SERIAL, arena);
                            if (value != null) {
                                serialMap.put(syspath, value);
                            }
                            MemorySegment parent = UdevFunctions.getParentWithSubsystemDevtype(device, SUBSYSTEM_USB,
                                    DEVTYPE_USB_DEVICE, arena);
                            if (MemorySegment.NULL.equals(parent)) {
                                usbControllers.add(syspath);
                            } else {
                                String parentPath = UdevFunctions
                                        .getString(UdevFunctions.udev_device_get_syspath(parent), arena);
                                if (parentPath != null) {
                                    hubMap.computeIfAbsent(parentPath, x -> new ArrayList<>()).add(syspath);
                                }
                            }
                        } finally {
                            UdevFunctions.udev_device_unref(device);
                        }
                    }
                } finally {
                    UdevFunctions.udev_enumerate_unref(enumerate);
                }
            } finally {
                UdevFunctions.udev_unref(udev);
            }
        } catch (Throwable e) {
            LOG.warn("Error enumerating USB devices: {}", e.getMessage());
            return emptyList();
        }

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }
}
