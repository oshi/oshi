/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static java.util.Collections.emptyList;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;
import static oshi.util.LogLevel.WARN;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.NativeHandle;
import oshi.ffm.platform.linux.UdevFunctions;
import oshi.hardware.common.platform.linux.UdevUsbDevice;

/**
 * Enumerates Linux USB devices via FFM/libudev, returning raw {@link UdevUsbDevice} attributes for
 * {@link oshi.hardware.common.platform.linux.LinuxUsbDevice} to assemble into a device tree.
 */
public final class LinuxUsbDeviceFFM {

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
     * Enumerates all {@code usb_device} entries via libudev.
     *
     * @return the raw USB device attributes, or an empty list if libudev is unavailable
     */
    public static List<UdevUsbDevice> queryUsbDevices() {
        if (!HAS_UDEV) {
            LOG.warn("USB Device information requires libudev, which is not present.");
            return emptyList();
        }
        return callInArenaOrDefault(arena -> {
            List<UdevUsbDevice> devices = new ArrayList<>();
            MemorySegment udev = UdevFunctions.udev_new();
            if (MemorySegment.NULL.equals(udev)) {
                return devices;
            }
            // wrapped only to release the native handle on close
            try (var _ = NativeHandle.of(udev, UdevFunctions::udev_unref)) {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                // wrapped only to release the native handle on close
                try (var _ = NativeHandle.of(enumerate, UdevFunctions::udev_enumerate_unref)) {
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
                        // wrapped only to release the native handle on close
                        try (var _ = NativeHandle.of(device, UdevFunctions::udev_device_unref)) {
                            String devtype = UdevFunctions.getString(UdevFunctions.udev_device_get_devtype(device),
                                    arena);
                            if (!DEVTYPE_USB_DEVICE.equals(devtype)) {
                                continue;
                            }
                            MemorySegment parent = UdevFunctions.getParentWithSubsystemDevtype(device, SUBSYSTEM_USB,
                                    DEVTYPE_USB_DEVICE, arena);
                            String parentPath = MemorySegment.NULL.equals(parent) ? null
                                    : UdevFunctions.getString(UdevFunctions.udev_device_get_syspath(parent), arena);
                            devices.add(new UdevUsbDevice(syspath,
                                    UdevFunctions.getSysattrValue(device, ATTR_PRODUCT, arena),
                                    UdevFunctions.getSysattrValue(device, ATTR_MANUFACTURER, arena),
                                    UdevFunctions.getSysattrValue(device, ATTR_VENDOR_ID, arena),
                                    UdevFunctions.getSysattrValue(device, ATTR_PRODUCT_ID, arena),
                                    UdevFunctions.getSysattrValue(device, ATTR_SERIAL, arena), parentPath));
                        }
                    }
                }
            }
            return devices;
        }, LOG, WARN, "Error enumerating USB devices", emptyList());
    }
}
