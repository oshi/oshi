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
package oshi.hardware.platform.unix.openbsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;

/**
 * OpenBsd Usb Device
 */
@Immutable
public class OpenBsdUsbDevice extends AbstractUsbDevice {

    public OpenBsdUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
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
            deviceList.add(new OpenBsdUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), device.getUniqueDeviceId(),
                    Collections.emptyList()));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static List<UsbDevice> getUsbDevices() {
        // Maps to store information using node # as the key
        // Node is controller+addr (+port+addr etc.)
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        List<String> rootHubs = new ArrayList<>();
        // For each item enumerated, store information in the maps
        String key = "";
        // Addresses repeat for each controller:
        // prepend the controller /dev/usb* for the key
        String parent = "";
        // Enumerate all devices and build information maps.
        // This will build the entire device tree in hubMap
        for (String line : ExecutingCommand.runNative("usbdevs -v")) {
            if (line.startsWith("Controller ")) {
                parent = line.substring(11);
            } else if (line.startsWith("addr ")) {
                // addr 01: 8086:0000 Intel, EHCI root hub
                if (line.indexOf(':') == 7 && line.indexOf(',') >= 18) {
                    key = parent + line.substring(0, 7);
                    String[] split = line.substring(8).trim().split(",");
                    if (split.length > 1) {
                        // 0 = vid:pid vendor
                        String vendorStr = split[0].trim();
                        int idx1 = vendorStr.indexOf(':');
                        int idx2 = vendorStr.indexOf(' ');
                        if (idx1 >= 0 && idx2 >= 0) {
                            vendorIdMap.put(key, vendorStr.substring(0, idx1));
                            productIdMap.put(key, vendorStr.substring(idx1 + 1, idx2));
                            vendorMap.put(key, vendorStr.substring(idx2 + 1));
                        }
                        // 1 = product
                        nameMap.put(key, split[1].trim());
                        // Add this key to the parent's hubmap list
                        hubMap.computeIfAbsent(parent, x -> new ArrayList<>()).add(key);
                        // For the first addr in a controller, make it the parent
                        if (!parent.contains("addr")) {
                            parent = key;
                            rootHubs.add(parent);
                        }
                    }
                }
            } else if (!key.isEmpty()) {
                // Continuing to read for the previous key
                // CSV is speed, power, config, rev, optional iSerial
                // Since all we need is the serial...
                int idx = line.indexOf("iSerial ");
                if (idx >= 0) {
                    serialMap.put(key, line.substring(idx + 8).trim());
                }
                key = "";
            }
        }

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String devusb : rootHubs) {
            controllerDevices.add(getDeviceAndChildren(devusb, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
        for (UsbDevice device : list) {
            deviceList.add(device);
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    /**
     * Recursively creates OpenBsdUsbDevices by fetching information from maps to
     * populate fields
     *
     * @param devPath
     *            The device node path.
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
     * @return A SolarisUsbDevice corresponding to this device
     */
    private static OpenBsdUsbDevice getDeviceAndChildren(String devPath, String vid, String pid,
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
        Collections.sort(usbDevices);
        return new OpenBsdUsbDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId),
                vendorMap.getOrDefault(devPath, ""), vendorId, productId, serialMap.getOrDefault(devPath, ""), devPath,
                usbDevices);
    }
}
