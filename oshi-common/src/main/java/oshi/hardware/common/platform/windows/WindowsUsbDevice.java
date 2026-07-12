/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quintet;
import oshi.util.tuples.Triplet;

/**
 * Windows USB device. The controller device tree (controller ids + parent/name/deviceId/manufacturer maps) is queried
 * by the backend — JNA {@code DeviceTree} or FFM {@code DeviceTreeFFM} — and supplied to {@link #getUsbDevices}; the
 * recursive tree assembly is shared between those backends here.
 * <p>
 * This intentionally does not use the shared {@code AbstractUsbDevice.buildDeviceTree}: the Windows model keys each
 * child to its parent (rather than parent-to-children), parses vendor/product/serial from PnP device-id strings, and
 * inherits serials from the parent.
 */
@Immutable
public final class WindowsUsbDevice extends AbstractUsbDevice {

    private WindowsUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Builds the USB controller device tree from a backend-supplied device-tree query.
     *
     * @param deviceTreeSupplier supplies the controller device tree: (controller ids, child→parent map, name map,
     *                           deviceId map, manufacturer map)
     * @return a list of USB controllers, each with its connected-device tree
     */
    public static List<UsbDevice> getUsbDevices(
            Supplier<Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>>> deviceTreeSupplier) {
        Quintet<Set<Integer>, Map<Integer, Integer>, Map<Integer, String>, Map<Integer, String>, Map<Integer, String>> controllerDevices = deviceTreeSupplier
                .get();
        Map<Integer, Integer> parentMap = controllerDevices.getB();
        Map<Integer, String> nameMap = controllerDevices.getC();
        Map<Integer, String> deviceIdMap = controllerDevices.getD();
        Map<Integer, String> mfgMap = controllerDevices.getE();

        List<UsbDevice> usbDevices = new ArrayList<>();
        // recursively build results
        for (Integer controllerDevice : controllerDevices.getA()) {
            WindowsUsbDevice deviceAndChildren = queryDeviceAndChildren(controllerDevice, parentMap, nameMap,
                    deviceIdMap, mfgMap, "0000", "0000", "");
            if (deviceAndChildren != null) {
                usbDevices.add(deviceAndChildren);
            }
        }
        return usbDevices;
    }

    private static WindowsUsbDevice queryDeviceAndChildren(Integer device, Map<Integer, Integer> parentMap,
            Map<Integer, String> nameMap, Map<Integer, String> deviceIdMap, Map<Integer, String> mfgMap, String vid,
            String pid, String parentSerial) {
        // Parse vendor and product IDs from the device ID; fall back to the parent's IDs if that fails
        String vendorId = vid;
        String productId = pid;
        String serial = parentSerial;
        Triplet<String, String, String> idsAndSerial = ParseUtil
                .parseDeviceIdToVendorProductSerial(deviceIdMap.get(device));
        if (idsAndSerial != null) {
            vendorId = idsAndSerial.getA();
            productId = idsAndSerial.getB();
            serial = idsAndSerial.getC();
            if (serial.isEmpty() && vendorId.equals(vid) && productId.equals(pid)) {
                serial = parentSerial;
            }
        }
        // Recursively find children in the parent map
        Set<Integer> childDeviceSet = parentMap.entrySet().stream().filter(e -> e.getValue().equals(device))
                .map(Entry::getKey).collect(Collectors.toSet());
        List<UsbDevice> childDevices = new ArrayList<>();
        for (Integer child : childDeviceSet) {
            WindowsUsbDevice deviceAndChildren = queryDeviceAndChildren(child, parentMap, nameMap, deviceIdMap, mfgMap,
                    vendorId, productId, serial);
            if (deviceAndChildren != null) {
                childDevices.add(deviceAndChildren);
            }
        }
        sort(childDevices);
        if (nameMap.containsKey(device)) {
            String name = nameMap.get(device);
            if (name.isEmpty()) {
                name = vendorId + ":" + productId;
            }
            String deviceId = deviceIdMap.get(device);
            String mfg = mfgMap.get(device);
            return new WindowsUsbDevice(name, mfg, vendorId, productId, serial, deviceId, childDevices);
        }
        return null;
    }
}
