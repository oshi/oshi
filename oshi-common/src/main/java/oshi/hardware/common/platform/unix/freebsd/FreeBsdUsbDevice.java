/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * FreeBsd Usb Device
 */
@Immutable
public class FreeBsdUsbDevice extends AbstractUsbDevice {

    public FreeBsdUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @return a list of USB controllers, each with its connected-device tree.
     */
    public static List<UsbDevice> getUsbDevices() {
        // Maps to store information using node # as the key
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        // Enumerate all devices and build information maps. This will build the
        // entire device tree; we will identify the controllers as the parents
        // of the usbus entries and eventually only populate the returned
        // results with those
        List<String> devices = ExecutingCommand.runNative("lshal");
        if (devices.isEmpty()) {
            return emptyList();
        }
        // For each item enumerated, store information in the maps
        String key = "";
        List<String> usBuses = new ArrayList<>();
        for (String line : devices) {
            // udi = ... identifies start of a new tree
            if (line.startsWith("udi =")) {
                // Remove indent for key
                key = ParseUtil.getSingleQuoteStringValue(line);
            } else if (!key.isEmpty()) {
                // We are currently processing for node identified by key. Save
                // approrpriate variables to maps.
                line = line.trim();
                if (!line.isEmpty()) {
                    if (line.startsWith("freebsd.driver =")
                            && "usbus".equals(ParseUtil.getSingleQuoteStringValue(line))) {
                        usBuses.add(key);
                    } else if (line.contains(".parent =")) {
                        String parent = ParseUtil.getSingleQuoteStringValue(line);
                        // If this is interface of parent, skip
                        if (key.replace(parent, "").startsWith("_if")) {
                            continue;
                        }
                        // Store parent for later usbus-skipping
                        parentMap.put(key, parent);
                        // Add this key to the parent's hubmap list
                        hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(key);
                    } else if (line.contains(".product =")) {
                        nameMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
                    } else if (line.contains(".vendor =")) {
                        vendorMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
                    } else if (line.contains(".serial =")) {
                        String serial = ParseUtil.getSingleQuoteStringValue(line);
                        serialMap.put(key,
                                serial.startsWith("0x") ? ParseUtil.hexStringToString(serial.replace("0x", ""))
                                        : serial);
                    } else if (line.contains(".vendor_id =")) {
                        vendorIdMap.put(key, String.format(Locale.ROOT, "%04x", ParseUtil.getFirstIntValue(line)));
                    } else if (line.contains(".product_id =")) {
                        productIdMap.put(key, String.format(Locale.ROOT, "%04x", ParseUtil.getFirstIntValue(line)));
                    }
                }
            }
        }

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String usbus : usBuses) {
            // Skip the usbuses: make their parents the controllers and replace
            // parents' children with the buses' children
            String parent = parentMap.get(usbus);
            hubMap.put(parent, new ArrayList<>(hubMap.getOrDefault(usbus, Collections.emptyList())));
            controllerDevices.add(buildDeviceTree(parent, "0000", "0000", nameMap, vendorMap, vendorIdMap, productIdMap,
                    serialMap, hubMap, FreeBsdUsbDevice::new));
        }
        return controllerDevices;
    }
}
