/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.mac.CoreFoundation; // NOSONAR squid:S1191
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.CoreFoundation.CFTypeRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

/**
 * Mac Usb Device
 */
@Immutable
public class MacUsbDevice extends AbstractUsbDevice {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private static final String IOUSB = "IOUSB";
    private static final String IOSERVICE = "IOService";

    public MacUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Instantiates a list of {@link oshi.hardware.UsbDevice} objects, representing
     * devices connected via a usb port (including internal devices).
     * <p>
     * If the value of {@code tree} is true, the top level devices returned from
     * this method are the USB Controllers; connected hubs and devices in its device
     * tree share that controller's bandwidth. If the value of {@code tree} is
     * false, USB devices (not controllers) are listed in a single flat list.
     *
     * @param tree
     *            If true, returns a list of controllers, which requires recursive
     *            iteration of connected devices. If false, returns a flat list of
     *            devices excluding controllers.
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = getUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        // Top level is controllers; they won't be added to the list, but all
        // their connected devices will be
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static List<UsbDevice> getUsbDevices() {
        // Maps to store information using RegistryEntryID as the key
        Map<Long, String> nameMap = new HashMap<>();
        Map<Long, String> vendorMap = new HashMap<>();
        Map<Long, String> vendorIdMap = new HashMap<>();
        Map<Long, String> productIdMap = new HashMap<>();
        Map<Long, String> serialMap = new HashMap<>();
        Map<Long, List<Long>> hubMap = new HashMap<>();

        List<Long> usbControllers = new ArrayList<>();
        IORegistryEntry root = IOKitUtil.getRoot();
        // Iterate over children of root in the IOUSB plane. This does not include
        // controllers so we have to check their parents
        IOIterator iter = root.getChildIterator(IOUSB);
        if (iter != null) {
            // Define keys
            CFStringRef locationIDKey = CFStringRef.createCFString("locationID");
            CFStringRef ioPropertyMatchKey = CFStringRef.createCFString("IOPropertyMatch");

            // Get the device directly under each controller
            IORegistryEntry device = iter.next();
            while (device != null) {
                long id = 0L;
                // The parent of this device in IOService plane is the controller
                IORegistryEntry controller = device.getParentEntry(IOSERVICE);
                if (controller != null) {
                    // Unique global identifier for this controller
                    id = controller.getRegistryEntryID();
                    // Populate other data for the controller
                    nameMap.put(id, controller.getName());
                    // The only information we have in registry for this controller is
                    // the locationID. Use that to search for matching PCI device to obtain
                    // more information for the controller
                    CFTypeRef ref = controller.createCFProperty(locationIDKey);
                    if (ref != null) {
                        getControllerIdByLocation(id, ref, locationIDKey, ioPropertyMatchKey, vendorIdMap,
                                productIdMap);
                        ref.release();
                    }
                    controller.release();
                }
                usbControllers.add(id);

                // Now recursively add this device and its children to the maps
                // id is the coontroller ID and the first parent ID
                addDeviceAndChildrenToMaps(device, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap,
                        hubMap);

                device.release();
                device = iter.next();
            }
            locationIDKey.release();
            ioPropertyMatchKey.release();
            iter.release();
        }
        root.release();

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (Long controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }

    /**
     * Recursively populate maps with information from a USB Device and its children
     *
     * @param device
     *            The device which, along with its children, should be added
     * @param parentId
     *            The id of the device's parent.
     * @param nameMap
     *            the map of names
     * @param vendorMap
     *            the map of vendors
     * @param vendorIdMap
     *            the map of vendorIds
     * @param productIdMap
     *            the map of productIds
     * @param serialMap
     *            the map of serial numbers
     * @param hubMap
     *            the map of hubs
     */
    private static void addDeviceAndChildrenToMaps(IORegistryEntry device, long parentId, Map<Long, String> nameMap,
            Map<Long, String> vendorMap, Map<Long, String> vendorIdMap, Map<Long, String> productIdMap,
            Map<Long, String> serialMap, Map<Long, List<Long>> hubMap) {

        // Unique global identifier for this device
        long id = device.getRegistryEntryID();
        // Store id as a child of parent in hubmap
        hubMap.computeIfAbsent(parentId, x -> new ArrayList<>()).add(id);
        // Get device name and store in map
        nameMap.put(id, device.getName().trim());
        // Get vendor and store in map
        String vendor = device.getStringProperty("USB Vendor Name");
        if (vendor != null) {
            vendorMap.put(id, vendor.trim());
        }
        // Get vendorId and store in map
        Long vendorId = device.getLongProperty("idVendor");
        if (vendorId != null) {
            vendorIdMap.put(id, String.format("%04x", 0xffff & vendorId));
        }
        // Get productId and store in map
        Long productId = device.getLongProperty("idProduct");
        if (productId != null) {
            productIdMap.put(id, String.format("%04x", 0xffff & productId));
        }
        // Get serial and store in map
        String serial = device.getStringProperty("USB Serial Number");
        if (serial != null) {
            serialMap.put(id, serial.trim());
        }

        // Now get this device's children (if any) and recurse
        IOIterator childIter = device.getChildIterator(IOUSB);
        IORegistryEntry childDevice = childIter.next();
        while (childDevice != null) {
            addDeviceAndChildrenToMaps(childDevice, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap,
                    hubMap);

            childDevice.release();
            childDevice = childIter.next();
        }
        childIter.release();
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
        for (UsbDevice device : list) {
            deviceList.add(
                    new MacUsbDevice(device.getName(), device.getVendor(), device.getVendorId(), device.getProductId(),
                            device.getSerialNumber(), device.getUniqueDeviceId(), Collections.emptyList()));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    /**
     * Looks up vendor and product id information for a USB Host Controller by
     * cross-referencing the location
     *
     * @param id
     *            The global unique ID for the host controller used as a key for
     *            maps
     * @param locationId
     *            The locationID of this controller returned from the registry
     * @param locationIDKey
     *            A pointer to the locationID string
     * @param ioPropertyMatchKey
     *            A pointer to the IOPropertyMatch string
     * @param productIdMap
     *            the map of productIds
     * @param vendorIdMap
     *            the map of vendorIds
     */
    private static void getControllerIdByLocation(long id, CFTypeRef locationId, CFStringRef locationIDKey,
            CFStringRef ioPropertyMatchKey, Map<Long, String> vendorIdMap, Map<Long, String> productIdMap) {
        // Create a matching property dictionary from the locationId
        CFMutableDictionaryRef propertyDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CFIndex(0),
                null, null);
        propertyDict.setValue(locationIDKey, locationId);
        CFMutableDictionaryRef matchingDict = CF.CFDictionaryCreateMutable(CF.CFAllocatorGetDefault(), new CFIndex(0),
                null, null);
        matchingDict.setValue(ioPropertyMatchKey, propertyDict);

        // search for all IOservices that match the locationID
        IOIterator serviceIterator = IOKitUtil.getMatchingServices(matchingDict);
        // getMatchingServices releases matchingDict
        propertyDict.release();

        // Iterate matching services looking for devices whose parents have the
        // vendor and device ids
        boolean found = false;
        if (serviceIterator != null) {
            IORegistryEntry matchingService = serviceIterator.next();
            while (matchingService != null && !found) {
                // Get the parent, which contains the keys we need
                IORegistryEntry parent = matchingService.getParentEntry(IOSERVICE);
                // look up the vendor-id by key
                // vendor-id is a byte array of 4 bytes
                if (parent != null) {
                    byte[] vid = parent.getByteArrayProperty("vendor-id");
                    if (vid != null && vid.length >= 2) {
                        vendorIdMap.put(id, String.format("%02x%02x", vid[1], vid[0]));
                        found = true;
                    }
                    // look up the device-id by key
                    // device-id is a byte array of 4 bytes
                    byte[] pid = parent.getByteArrayProperty("device-id");
                    if (pid != null && pid.length >= 2) {
                        productIdMap.put(id, String.format("%02x%02x", pid[1], pid[0]));
                        found = true;
                    }
                    parent.release();
                }
                // iterate
                matchingService.release();
                matchingService = serviceIterator.next();
            }
            serviceIterator.release();
        }
    }

    /**
     * Recursively creates MacUsbDevices by fetching information from maps to
     * populate fields
     *
     * @param registryEntryId
     *            The device unique registry id.
     * @param vid
     *            The default (parent) vendor ID
     * @param pid
     *            The default (parent) product ID
     * @param nameMap
     *            the map of names
     * @param vendorMap
     *            the map of vendors
     * @param vendorIdMap
     *            the map of vendorIds
     * @param productIdMap
     *            the map of productIds
     * @param serialMap
     *            the map of serial numbers
     * @param hubMap
     *            the map of hubs
     * @return A MacUsbDevice corresponding to this device
     */
    private static MacUsbDevice getDeviceAndChildren(Long registryEntryId, String vid, String pid,
            Map<Long, String> nameMap, Map<Long, String> vendorMap, Map<Long, String> vendorIdMap,
            Map<Long, String> productIdMap, Map<Long, String> serialMap, Map<Long, List<Long>> hubMap) {
        String vendorId = vendorIdMap.getOrDefault(registryEntryId, vid);
        String productId = productIdMap.getOrDefault(registryEntryId, pid);
        List<Long> childIds = hubMap.getOrDefault(registryEntryId, new ArrayList<>());
        List<UsbDevice> usbDevices = new ArrayList<>();
        for (Long id : childIds) {
            usbDevices.add(getDeviceAndChildren(id, vendorId, productId, nameMap, vendorMap, vendorIdMap, productIdMap,
                    serialMap, hubMap));
        }
        Collections.sort(usbDevices);
        return new MacUsbDevice(nameMap.getOrDefault(registryEntryId, vendorId + ":" + productId),
                vendorMap.getOrDefault(registryEntryId, ""), vendorId, productId,
                serialMap.getOrDefault(registryEntryId, ""), "0x" + Long.toHexString(registryEntryId), usbDevices);
    }
}
