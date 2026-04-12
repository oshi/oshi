/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import static java.util.Collections.emptyList;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.mac.CoreFoundation.CFAllocatorRef;
import oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundation.CFTypeRef;
import oshi.ffm.mac.CoreFoundationFunctions;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.mac.MacUsbDevice;
import oshi.util.platform.mac.IOKitUtilFFM;

/**
 * Mac USB device helper using FFM/IOKit. Instantiates {@link MacUsbDevice} objects.
 */
public final class MacUsbDeviceFFM extends MacUsbDevice {

    private MacUsbDeviceFFM() {
    }

    private static final String IOUSB = "IOUSB";
    private static final String IOSERVICE = "IOService";
    private static final Logger LOG = LoggerFactory.getLogger(MacUsbDeviceFFM.class);

    /**
     * Instantiates a list of {@link oshi.hardware.UsbDevice} objects, representing devices connected via a usb port
     * (including internal devices).
     *
     * @param tree If true, returns a list of controllers with their device tree. If false, returns a flat list of
     *             devices excluding controllers.
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = queryUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static List<UsbDevice> queryUsbDevices() {
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> vendorMap = new HashMap<>();
        Map<Long, String> vendorIdMap = new HashMap<>();
        Map<Long, String> productIdMap = new HashMap<>();
        Map<Long, String> serialMap = new HashMap<>();
        Map<Long, List<Long>> hubMap = new HashMap<>();

        Set<Long> usbControllers = new LinkedHashSet<>();
        IORegistryEntry root = IOKitUtilFFM.getRoot();
        if (root == null) {
            return emptyList();
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
                    // If controller is null, id remains 0L, acting as an anonymous catch-all
                    // controller so that devices whose parent cannot be identified are not lost.
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

    private static void getControllerIdByLocation(long id, CFTypeRef locationId, CFStringRef locationIDKey,
            CFStringRef ioPropertyMatchKey, Map<Long, String> vendorIdMap, Map<Long, String> productIdMap) {
        // Build matching dict: { IOPropertyMatch: { locationID: <locationId> } }
        try {
            CFAllocatorRef alloc = new CFAllocatorRef(CoreFoundationFunctions.CFAllocatorGetDefault());
            CFMutableDictionaryRef propertyDict = new CFMutableDictionaryRef(CoreFoundationFunctions
                    .CFDictionaryCreateMutable(alloc.segment(), 0, MemorySegment.NULL, MemorySegment.NULL));
            propertyDict.setValue(locationIDKey, locationId);
            CFMutableDictionaryRef matchingDict = new CFMutableDictionaryRef(CoreFoundationFunctions
                    .CFDictionaryCreateMutable(alloc.segment(), 0, MemorySegment.NULL, MemorySegment.NULL));
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
            LOG.debug("Failed to retrieve controller vendor/product IDs for id {}", id, e);
        }
    }
}
