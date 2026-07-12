/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

/**
 * A Linux USB device. Backends (udev via JNA or FFM, or the native-free sysfs reader) enumerate raw
 * {@link UdevUsbDevice} attributes; this class builds the controller device tree from them, so all the
 * parsing/parent-child/sorting logic lives in one place.
 */
@Immutable
public final class LinuxUsbDevice extends AbstractUsbDevice {

    private LinuxUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Builds the USB controller device tree from raw per-device attributes gathered by a backend.
     *
     * @param rawDevices the enumerated {@code usb_device} entries, in any order
     * @return a list of USB controllers, each with its connected-device tree
     */
    public static List<UsbDevice> getUsbDevices(List<UdevUsbDevice> rawDevices) {
        // Build lookup maps keyed by syspath, plus the parent -> children (hub) map and the list of root controllers
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();
        List<String> usbControllers = new ArrayList<>();
        for (UdevUsbDevice device : rawDevices) {
            String syspath = device.getSyspath();
            if (device.getProduct() != null) {
                nameMap.put(syspath, device.getProduct());
            }
            if (device.getManufacturer() != null) {
                vendorMap.put(syspath, device.getManufacturer());
            }
            if (device.getVendorId() != null) {
                vendorIdMap.put(syspath, device.getVendorId());
            }
            if (device.getProductId() != null) {
                productIdMap.put(syspath, device.getProductId());
            }
            if (device.getSerial() != null) {
                serialMap.put(syspath, device.getSerial());
            }
            String parent = device.getParentSyspath();
            if (parent == null) {
                usbControllers.add(syspath);
            } else {
                hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(syspath);
            }
        }

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(buildDeviceTree(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap, LinuxUsbDevice::new));
        }
        return controllerDevices;
    }

}
