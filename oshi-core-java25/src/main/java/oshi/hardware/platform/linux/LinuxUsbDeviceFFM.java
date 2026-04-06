/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.linux.UdevFunctions;
import oshi.hardware.UsbDevice;

/**
 * FFM-based Linux USB device implementation.
 */
@Immutable
public class LinuxUsbDeviceFFM extends LinuxUsbDevice {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxUsbDeviceFFM.class);

    public LinuxUsbDeviceFFM(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Returns a list of USB devices using FFM-based udev enumeration.
     *
     * @param tree If true, returns a controller tree; if false, returns a flat list.
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        return new LinuxUsbDeviceFFM("", "", "", "", "", "", Collections.emptyList()).queryUsbDevices(tree);
    }

    @Override
    protected LinuxUsbDevice createDevice(String name, String vendor, String vendorId, String productId,
            String serialNumber, String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        return new LinuxUsbDeviceFFM(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    @Override
    protected void enumerateUsbDevices(List<String> usbControllers, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap) {
        if (!HAS_UDEV) {
            LOG.warn("USB Device information requires libudev, which is not present.");
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return;
            }
            try {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                try {
                    UdevFunctions.addMatchSubsystem(enumerate, SUBSYSTEM_USB, arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);
                    for (MemorySegment entry = UdevFunctions
                            .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                    .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
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
        }
    }
}
