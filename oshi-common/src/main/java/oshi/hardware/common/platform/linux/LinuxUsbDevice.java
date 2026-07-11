/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static java.util.Collections.sort;

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
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }

    /**
     * Recursively creates a LinuxUsbDevice by fetching information from the maps to populate its fields.
     *
     * @param devPath      the syspath of the device to create
     * @param vid          the default vendor ID, inherited from the parent if this device has none
     * @param pid          the default product ID, inherited from the parent if this device has none
     * @param nameMap      syspath to product name
     * @param vendorMap    syspath to manufacturer name
     * @param vendorIdMap  syspath to vendor ID
     * @param productIdMap syspath to product ID
     * @param serialMap    syspath to serial number
     * @param hubMap       parent syspath to its child syspaths
     * @return a {@link LinuxUsbDevice} for {@code devPath} with its connected devices populated recursively
     */
    static LinuxUsbDevice getDeviceAndChildren(String devPath, String vid, String pid, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap) {
        String vendorId = vendorIdMap.getOrDefault(devPath, vid);
        String productId = productIdMap.getOrDefault(devPath, pid);
        List<String> childPaths = hubMap.getOrDefault(devPath, new ArrayList<>());
        List<UsbDevice> usbDevices = new ArrayList<>();
        for (String path : childPaths) {
            usbDevices.add(getDeviceAndChildren(path, vendorId, productId, nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        sort(usbDevices);
        return new LinuxUsbDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId),
                vendorMap.getOrDefault(devPath, ""), vendorId, productId, serialMap.getOrDefault(devPath, ""), devPath,
                usbDevices);
    }
}
