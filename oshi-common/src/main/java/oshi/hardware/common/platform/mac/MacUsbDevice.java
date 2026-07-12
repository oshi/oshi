/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

/**
 * A Mac USB device. Backends (IOKit via JNA or FFM) supply a {@link RegistryEntry} view of the IO registry root; this
 * class walks it once and builds the controller device tree, so all the parsing/parent-child/sorting logic lives in one
 * place.
 */
@Immutable
public final class MacUsbDevice extends AbstractUsbDevice {

    private static final String IOUSB = "IOUSB";
    private static final String IOSERVICE = "IOService";

    private MacUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * A backend-neutral view of an IOKit registry entry, letting the shared walk in {@link MacUsbDevice} run against
     * either the JNA or FFM IOKit bindings. Each backend adapts its native registry-entry wrapper to this interface.
     */
    public interface RegistryEntry extends AutoCloseable {
        /**
         * @return the entry's globally-unique registry ID
         */
        long getRegistryEntryID();

        /**
         * @return the entry's name, or {@code null}
         */
        String getName();

        /**
         * @param key the property key
         * @return the string-valued property, or {@code null} if absent
         */
        String getStringProperty(String key);

        /**
         * @param key the property key
         * @return the long-valued property, or {@code null} if absent
         */
        Long getLongProperty(String key);

        /**
         * @param plane the registry plane to search
         * @return the parent entry in the given plane, or {@code null} if none
         */
        RegistryEntry getParentEntry(String plane);

        /**
         * @param plane the registry plane to iterate
         * @return an iterator over child entries in the given plane, or {@code null} if none
         */
        RegistryIterator getChildIterator(String plane);

        /**
         * Resolves this (controller) entry's vendor and product IDs by cross-referencing its {@code locationID} against
         * the matching PCI device, encapsulating the backend-specific CoreFoundation dictionary matching.
         *
         * @return a two-element array {@code {vendorId, productId}}; either element may be {@code null} if not found
         */
        String[] lookupControllerVidPid();

        /**
         * Releases the underlying native entry.
         */
        void release();

        @Override
        default void close() {
            release();
        }
    }

    /**
     * A backend-neutral view of an IOKit registry iterator.
     */
    public interface RegistryIterator extends AutoCloseable {
        /**
         * @return the next entry, or {@code null} when the iterator is exhausted
         */
        RegistryEntry next();

        /**
         * Releases the underlying native iterator.
         */
        void release();

        @Override
        default void close() {
            release();
        }
    }

    /**
     * Walks the IO registry from {@code root} and builds the USB controller device tree.
     *
     * @param root the IO registry root as a backend-neutral view, or {@code null} if unavailable
     * @return a list of USB controllers, each with its connected-device tree
     */
    public static List<UsbDevice> getUsbDevices(RegistryEntry root) {
        if (root == null) {
            return emptyList();
        }
        // Maps to store information using the RegistryEntryID (as a "0x"-prefixed hex string) as the key
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();
        Set<String> usbControllers = new LinkedHashSet<>();

        // Iterate over children of root in the IOUSB plane. This does not include the controllers, so we check each
        // device's parent in the IOService plane.
        RegistryIterator iter = root.getChildIterator(IOUSB);
        if (iter != null) {
            RegistryEntry device = iter.next();
            while (device != null) {
                // Anonymous catch-all controller key when the parent can't be identified
                String id = toKey(0L);
                // The parent of this device in the IOService plane is the controller
                RegistryEntry controller = device.getParentEntry(IOSERVICE);
                if (controller != null) {
                    id = toKey(controller.getRegistryEntryID());
                    // Devices sharing a controller yield the same deterministic name/vid/pid, so only read them the
                    // first time this controller is seen; later devices still register under it via usbControllers.
                    if (!usbControllers.contains(id)) {
                        // Skip a null name so buildDeviceTree can fall back to vid:pid (getOrDefault only substitutes
                        // for absent keys, not null values)
                        String controllerName = controller.getName();
                        if (controllerName != null) {
                            nameMap.put(id, controllerName);
                        }
                        // The only controller info in the registry is its locationID; use it to find the matching PCI
                        // device and pull vendor/product IDs.
                        String[] vidPid = controller.lookupControllerVidPid();
                        if (vidPid[0] != null) {
                            vendorIdMap.put(id, vidPid[0]);
                        }
                        if (vidPid[1] != null) {
                            productIdMap.put(id, vidPid[1]);
                        }
                    }
                    controller.release();
                }
                // If controller is null, id remains the toKey(0L) catch-all controller so that devices whose parent
                // cannot be identified are not lost.
                usbControllers.add(id);
                // Recursively add this device and its children to the maps, keyed under the controller id
                addDeviceAndChildrenToMaps(device, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap,
                        hubMap);
                device.release();
                device = iter.next();
            }
            iter.release();
        }
        root.release();

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(buildDeviceTree(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap, MacUsbDevice::new));
        }
        return controllerDevices;
    }

    /**
     * Formats a registry entry ID as the {@code "0x"}-prefixed hex string used as the map key and unique device ID.
     *
     * @param registryEntryId the registry entry ID
     * @return the formatted key
     */
    private static String toKey(long registryEntryId) {
        return "0x" + Long.toHexString(registryEntryId);
    }

    /**
     * Recursively populates the maps with information from a USB device and its children.
     *
     * @param device       the device which, along with its children, should be added
     * @param parentId     the id of the device's parent
     * @param nameMap      the map of names
     * @param vendorMap    the map of vendors
     * @param vendorIdMap  the map of vendorIds
     * @param productIdMap the map of productIds
     * @param serialMap    the map of serial numbers
     * @param hubMap       the map of hubs
     */
    private static void addDeviceAndChildrenToMaps(RegistryEntry device, String parentId, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap) {
        // Unique global identifier for this device
        String id = toKey(device.getRegistryEntryID());
        // Store id as a child of parent in the hub map
        hubMap.computeIfAbsent(parentId, x -> new ArrayList<>()).add(id);
        // Get device name and store in map (skip if null so the walk isn't aborted before native handles are released
        // and buildDeviceTree can fall back to vid:pid)
        String name = device.getName();
        if (name != null) {
            nameMap.put(id, name.trim());
        }
        // Get vendor and store in map
        String vendor = device.getStringProperty("USB Vendor Name");
        if (vendor != null) {
            vendorMap.put(id, vendor.trim());
        }
        // Get vendorId and store in map
        Long vendorId = device.getLongProperty("idVendor");
        if (vendorId != null) {
            vendorIdMap.put(id, String.format(Locale.ROOT, "%04x", 0xffff & vendorId));
        }
        // Get productId and store in map
        Long productId = device.getLongProperty("idProduct");
        if (productId != null) {
            productIdMap.put(id, String.format(Locale.ROOT, "%04x", 0xffff & productId));
        }
        // Get serial and store in map
        String serial = device.getStringProperty("USB Serial Number");
        if (serial != null) {
            serialMap.put(id, serial.trim());
        }

        // Now get this device's children (if any) and recurse
        RegistryIterator childIter = device.getChildIterator(IOUSB);
        if (childIter != null) {
            RegistryEntry childDevice = childIter.next();
            while (childDevice != null) {
                addDeviceAndChildrenToMaps(childDevice, id, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap,
                        hubMap);
                childDevice.release();
                childDevice = childIter.next();
            }
            childIter.release();
        }
    }

}
