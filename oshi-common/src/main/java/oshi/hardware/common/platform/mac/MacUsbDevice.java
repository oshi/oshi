/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static java.util.Collections.emptyList;
import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

/**
 * Mac USB device base class.
 */
@Immutable
public class MacUsbDevice extends AbstractUsbDevice {

    protected MacUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * No-arg constructor required so that subclasses, which extend this class solely to inherit its static helper
     * methods, can compile without an explicit constructor. No subclass is ever instantiated; only this class is, via
     * the full constructor.
     */
    protected MacUsbDevice() {
        this("", "", "", "", "", "", emptyList());
    }

    /**
     * Recursively creates MacUsbDevices by fetching information from maps to populate fields
     *
     * @param registryEntryId The device unique registry id.
     * @param vid             The default (parent) vendor ID
     * @param pid             The default (parent) product ID
     * @param nameMap         the map of names
     * @param vendorMap       the map of vendors
     * @param vendorIdMap     the map of vendorIds
     * @param productIdMap    the map of productIds
     * @param serialMap       the map of serial numbers
     * @param hubMap          the map of hubs
     * @return A MacUsbDevice corresponding to this device
     */
    protected static MacUsbDevice getDeviceAndChildren(Long registryEntryId, String vid, String pid,
            Map<Long, String> nameMap, Map<Long, String> vendorMap, Map<Long, String> vendorIdMap,
            Map<Long, String> productIdMap, Map<Long, String> serialMap, Map<Long, List<Long>> hubMap) {
        String vendorId = vendorIdMap.getOrDefault(registryEntryId, vid);
        String productId = productIdMap.getOrDefault(registryEntryId, pid);
        List<Long> childIds = hubMap.getOrDefault(registryEntryId, new ArrayList<>());
        List<UsbDevice> usbDevices = new ArrayList<>();
        for (Long childId : childIds) {
            usbDevices.add(getDeviceAndChildren(childId, vendorId, productId, nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        sort(usbDevices);
        return new MacUsbDevice(nameMap.getOrDefault(registryEntryId, vendorId + ":" + productId),
                vendorMap.getOrDefault(registryEntryId, ""), vendorId, productId,
                serialMap.getOrDefault(registryEntryId, ""), "0x" + Long.toHexString(registryEntryId), usbDevices);
    }
}
