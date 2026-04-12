/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

/**
 * Linux USB device base class.
 */
@Immutable
public class LinuxUsbDevice extends AbstractUsbDevice {

    protected LinuxUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * No-arg constructor required so that subclasses, which extend this class solely to inherit its static helper
     * methods, can compile without an explicit constructor. No subclass is ever instantiated; only this class is, via
     * the full constructor.
     */
    protected LinuxUsbDevice() {
        this("", "", "", "", "", "", emptyList());
    }

    /**
     * Recursively creates LinuxUsbDevices by fetching information from maps to populate fields
     *
     * @param devPath      The device node path.
     * @param vid          The default (parent) vendor ID
     * @param pid          The default (parent) product ID
     * @param nameMap      the map of names
     * @param vendorMap    the map of vendors
     * @param vendorIdMap  the map of vendorIds
     * @param productIdMap the map of productIds
     * @param serialMap    the map of serial numbers
     * @param hubMap       the map of hubs
     * @return A LinuxUsbDevice corresponding to this device
     */
    protected static LinuxUsbDevice getDeviceAndChildren(String devPath, String vid, String pid,
            Map<String, String> nameMap, Map<String, String> vendorMap, Map<String, String> vendorIdMap,
            Map<String, String> productIdMap, Map<String, String> serialMap, Map<String, List<String>> hubMap) {
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
