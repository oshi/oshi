/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static java.util.Collections.emptyList;
import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.hardware.common.platform.linux.UdevUsbDevice;

/**
 * Enumerates Linux USB devices via JNA/libudev, returning raw {@link UdevUsbDevice} attributes for
 * {@link oshi.hardware.common.platform.linux.LinuxUsbDevice} to assemble into a device tree.
 */
public final class LinuxUsbDeviceJNA {

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
     * Enumerates all {@code usb_device} entries via libudev.
     *
     * @return the raw USB device attributes, or an empty list if libudev is unavailable
     */
    public static List<UdevUsbDevice> queryUsbDevices() {
        if (!HAS_UDEV) {
            LOG.warn("USB Device information requires libudev, which is not present.");
            return emptyList();
        }
        List<UdevUsbDevice> devices = new ArrayList<>();
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
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            // Only include usb_device devtype, skipping usb_interface
                            if (DEVTYPE_USB_DEVICE.equals(device.getDevtype())) {
                                UdevDevice parent = device.getParentWithSubsystemDevtype(SUBSYSTEM_USB,
                                        DEVTYPE_USB_DEVICE);
                                devices.add(new UdevUsbDevice(syspath, device.getSysattrValue(ATTR_PRODUCT),
                                        device.getSysattrValue(ATTR_MANUFACTURER),
                                        device.getSysattrValue(ATTR_VENDOR_ID), device.getSysattrValue(ATTR_PRODUCT_ID),
                                        device.getSysattrValue(ATTR_SERIAL),
                                        parent == null ? null : parent.getSyspath()));
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
        return devices;
    }
}
