/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.platform.mac.IOKitUtilFFM;

/**
 * Mac Usb Device FFM implementation.
 */
@Immutable
public class MacUsbDeviceFFM extends AbstractUsbDevice {

    private static final String IOUSB = "IOUSB";
    private static final String IOSERVICE = "IOService";

    public MacUsbDeviceFFM(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    public static List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = getUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static List<UsbDevice> getUsbDevices() {
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> vendorMap = new HashMap<>();
        Map<Long, String> vendorIdMap = new HashMap<>();
        Map<Long, String> productIdMap = new HashMap<>();
        Map<Long, String> serialMap = new HashMap<>();
        Map<Long, List<Long>> hubMap = new HashMap<>();

        List<Long> usbControllers = new ArrayList<>();
        IORegistryEntry root = IOKitUtilFFM.getRoot();
        if (root == null) {
            return Collections.emptyList();
        }
        IOIterator iter = root.getChildIterator(IOUSB);
        if (iter != null) {
            CFStringRef locationIDKey = CFStringRef.createCFString("locationID");
            CFStringRef ioPropertyMatchKey = CFStringRef.createCFString("IOPropertyMatch");
            try {
                IORegistryEntry device = iter.next();
                while (device != null) {
                    long id = 0L;
                    IORegistryEntry controller = device.getParentEntry(IOSERVICE);
                    if (controller != null) {
                        id = controller.getRegistryEntryID();
                        nameMap.put(id, controller.getName());
                        MemorySegment ref = controller.createCFProperty(locationIDKey.segment());
                        if (ref != null && !ref.equals(MemorySegment.NULL)) {
                            CFTypeRef locationId = new CFTypeRef(ref);
                            getControllerIdByLocation(id, locationId, locationIDKey, ioPropertyMatchKey, vendorIdMap,
                                    productIdMap);
                            locationId.release();
                        }
                        controller.release();
                    }
                    usbControllers.add(id);
                    addDeviceAndChildrenToMaps(device, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap,
                            hubMap);
                    device.release();
                    device = iter.next();
                }
            } finally {
                locationIDKey.release();
                ioPropertyMatchKey.release();
                iter.release();
            }
        }
        root.release();

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (Long controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }

    private static void addDeviceAndChildrenToMaps(IORegistryEntry device, long parentId, Map<Long, String> nameMap,
            Map<Long, String> vendorMap, Map<Long, String> vendorIdMap, Map<Long, String> productIdMap,
            Map<Long, String> serialMap, Map<Long, List<Long>> hubMap) {
        long id = device.getRegistryEntryID();
        hubMap.computeIfAbsent(parentId, x -> new ArrayList<>()).add(id);
        nameMap.put(id, device.getName().trim());
        String vendor = device.getStringProperty("USB Vendor Name");
        if (vendor != null) {
            vendorMap.put(id, vendor.trim());
        }
        Long vendorId = device.getLongProperty("idVendor");
        if (vendorId != null) {
            vendorIdMap.put(id, String.format(Locale.ROOT, "%04x", 0xffff & vendorId));
        }
        Long productId = device.getLongProperty("idProduct");
        if (productId != null) {
            productIdMap.put(id, String.format(Locale.ROOT, "%04x", 0xffff & productId));
        }
        String serial = device.getStringProperty("USB Serial Number");
        if (serial != null) {
            serialMap.put(id, serial.trim());
        }

        IOIterator childIter = device.getChildIterator(IOUSB);
        if (childIter != null) {
            try {
                IORegistryEntry childDevice = childIter.next();
                while (childDevice != null) {
                    addDeviceAndChildrenToMaps(childDevice, id, nameMap, vendorMap, vendorIdMap, productIdMap,
                            serialMap, hubMap);
                    childDevice.release();
                    childDevice = childIter.next();
                }
            } finally {
                childIter.release();
            }
        }
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
        for (UsbDevice device : list) {
            deviceList.add(new MacUsbDeviceFFM(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), device.getUniqueDeviceId(),
                    Collections.emptyList()));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static void getControllerIdByLocation(long id, CFTypeRef locationId, CFStringRef locationIDKey,
            CFStringRef ioPropertyMatchKey, Map<Long, String> vendorIdMap, Map<Long, String> productIdMap) {
        // Build matching dict: { IOPropertyMatch: { locationID: <locationId> } }
        try {
            oshi.ffm.mac.CoreFoundation.CFAllocatorRef alloc = new oshi.ffm.mac.CoreFoundation.CFAllocatorRef(
                    oshi.ffm.mac.CoreFoundationFunctions.CFAllocatorGetDefault());
            oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef propertyDict = new oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef(
                    oshi.ffm.mac.CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                            MemorySegment.NULL, MemorySegment.NULL));
            propertyDict.setValue(locationIDKey, locationId);
            oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef matchingDict = new oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef(
                    oshi.ffm.mac.CoreFoundationFunctions.CFDictionaryCreateMutable(alloc.segment(), 0,
                            MemorySegment.NULL, MemorySegment.NULL));
            matchingDict.setValue(ioPropertyMatchKey, propertyDict);

            IOIterator serviceIterator = IOKitUtilFFM.getMatchingServices(matchingDict.segment());
            propertyDict.release();

            boolean found = false;
            if (serviceIterator != null) {
                try {
                    IORegistryEntry matchingService = serviceIterator.next();
                    while (matchingService != null && !found) {
                        IORegistryEntry parent = matchingService.getParentEntry(IOSERVICE);
                        if (parent != null) {
                            byte[] vid = parent.getByteArrayProperty("vendor-id");
                            if (vid != null && vid.length >= 2) {
                                vendorIdMap.put(id, String.format(Locale.ROOT, "%02x%02x", vid[1], vid[0]));
                                found = true;
                            }
                            byte[] pid = parent.getByteArrayProperty("device-id");
                            if (pid != null && pid.length >= 2) {
                                productIdMap.put(id, String.format(Locale.ROOT, "%02x%02x", pid[1], pid[0]));
                                found = true;
                            }
                            parent.release();
                        }
                        matchingService.release();
                        matchingService = serviceIterator.next();
                    }
                } finally {
                    serviceIterator.release();
                }
            }
        } catch (Throwable e) {
            // Ignore
        }
    }

    private static MacUsbDeviceFFM getDeviceAndChildren(Long registryEntryId, String vid, String pid,
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
        Collections.sort(usbDevices);
        return new MacUsbDeviceFFM(nameMap.getOrDefault(registryEntryId, vendorId + ":" + productId),
                vendorMap.getOrDefault(registryEntryId, ""), vendorId, productId,
                serialMap.getOrDefault(registryEntryId, ""), "0x" + Long.toHexString(registryEntryId), usbDevices);
    }
}
