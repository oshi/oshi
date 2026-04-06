/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;

/**
 * JNA-based Linux USB device implementation.
 */
@Immutable
public class LinuxUsbDeviceJNA extends LinuxUsbDevice {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxUsbDeviceJNA.class);

    public LinuxUsbDeviceJNA(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Returns a list of USB devices using JNA-based udev enumeration.
     *
     * @param tree If true, returns a controller tree; if false, returns a flat list.
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        return new LinuxUsbDeviceJNA("", "", "", "", "", "", Collections.emptyList()).queryUsbDevices(tree);
    }

    @Override
    protected LinuxUsbDevice createDevice(String name, String vendor, String vendorId, String productId,
            String serialNumber, String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        return new LinuxUsbDeviceJNA(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    @Override
    protected void enumerateUsbDevices(List<String> usbControllers, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap) {
        if (!HAS_UDEV) {
            LOG.warn("USB Device information requires libudev, which is not present.");
            return;
        }
        Udev.UdevContext udev = Udev.INSTANCE.udev_new();
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
                            if (!DEVTYPE_USB_DEVICE.equals(device.getDevtype())) {
                                continue;
                            }
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
                            UdevDevice parent = device.getParentWithSubsystemDevtype(SUBSYSTEM_USB, DEVTYPE_USB_DEVICE);
                            if (parent == null) {
                                usbControllers.add(syspath);
                            } else {
                                hubMap.computeIfAbsent(parent.getSyspath(), x -> new ArrayList<>()).add(syspath);
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
    }
}
