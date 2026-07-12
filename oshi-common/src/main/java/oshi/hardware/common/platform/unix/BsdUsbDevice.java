/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;

/**
 * Shared USB device implementation for BSDs that use {@code usbdevs -v} to enumerate devices (NetBSD, OpenBSD).
 */
@Immutable
public final class BsdUsbDevice extends AbstractUsbDevice {

    public BsdUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @return a list of USB controllers, each with its connected-device tree.
     */
    public static List<UsbDevice> getUsbDevices() {
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        List<String> rootHubs = new ArrayList<>();
        String key = "";
        String parent = "";
        for (String line : ExecutingCommand.runNative("usbdevs -v")) {
            if (line.startsWith("Controller ")) {
                parent = line.substring(11);
            } else if (line.startsWith("addr ")) {
                if (line.indexOf(':') == 7 && line.indexOf(',') >= 18) {
                    key = parent + line.substring(0, 7);
                    String[] split = line.substring(8).trim().split(",");
                    if (split.length > 1) {
                        String vendorStr = split[0].trim();
                        int idx1 = vendorStr.indexOf(':');
                        int idx2 = vendorStr.indexOf(' ');
                        if (idx1 >= 0 && idx2 >= 0) {
                            vendorIdMap.put(key, vendorStr.substring(0, idx1));
                            productIdMap.put(key, vendorStr.substring(idx1 + 1, idx2));
                            vendorMap.put(key, vendorStr.substring(idx2 + 1));
                        }
                        nameMap.put(key, split[1].trim());
                        hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(key);
                        if (!parent.contains("addr")) {
                            parent = key;
                            rootHubs.add(parent);
                        }
                    }
                }
            } else if (!key.isEmpty()) {
                int idx = line.indexOf("iSerial ");
                if (idx >= 0) {
                    serialMap.put(key, line.substring(idx + 8).trim());
                }
                key = "";
            }
        }

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String devusb : rootHubs) {
            controllerDevices.add(buildDeviceTree(devusb, "0000", "0000", nameMap, vendorMap, vendorIdMap, productIdMap,
                    serialMap, hubMap, BsdUsbDevice::new));
        }
        return controllerDevices;
    }
}
