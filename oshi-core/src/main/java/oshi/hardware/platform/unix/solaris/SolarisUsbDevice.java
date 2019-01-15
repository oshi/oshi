/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.solaris;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public class SolarisUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    /*
     * Maps to store information using node # as the key
     */
    private static Map<String, String> nameMap = new HashMap<>();
    private static Map<String, String> vendorIdMap = new HashMap<>();
    private static Map<String, String> productIdMap = new HashMap<>();
    private static Map<String, List<String>> hubMap = new HashMap<>();
    private static Map<String, String> deviceTypeMap = new HashMap<>();
    /*
     * For parsing tree
     */
    private static Map<Integer, String> lastParent = new HashMap<>();

    public SolarisUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            UsbDevice[] connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, connectedDevices);
    }

    /**
     * {@inheritDoc}
     */
    public static UsbDevice[] getUsbDevices(boolean tree) {
        UsbDevice[] devices = getUsbDevices();
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        // Top level is controllers; they won't be added to the list, but all
        // their connected devices will be
        for (UsbDevice device : devices) {
            deviceList.add(new SolarisUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), new SolarisUsbDevice[0]));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList.toArray(new UsbDevice[deviceList.size()]);
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, UsbDevice[] connectedDevices) {
        for (UsbDevice device : connectedDevices) {
            deviceList.add(device);
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static UsbDevice[] getUsbDevices() {
        // Empty out maps
        nameMap.clear();
        vendorIdMap.clear();
        productIdMap.clear();
        hubMap.clear();

        // Enumerate all usb devices and build information maps
        List<String> devices = ExecutingCommand.runNative("prtconf -pv");
        if (devices.isEmpty()) {
            return new SolarisUsbDevice[0];
        }
        // For each item enumerated, store information in the maps
        String key = "";
        int indent = 0;
        List<String> usbControllers = new ArrayList<>();
        for (String line : devices) {
            // Node 0x... identifies start of a new tree
            if (line.contains("Node 0x")) {
                // Remove indent for key
                key = line.replaceFirst("^\\s*", "");
                // Calculate indent and store as last parent at this depth
                int depth = line.length() - key.length();
                // Store first indent for future use
                if (indent == 0) {
                    indent = depth;
                }
                // Store this Node ID as parent at this depth
                lastParent.put(depth, key);
                // Add as child to appropriate parent
                if (depth > indent) {
                    // Has a parent. Get parent and add this node to child list
                    hubMap.computeIfAbsent(lastParent.get(depth - indent), x -> new ArrayList<>()).add(key);
                } else {
                    // No parent, add to controllers list
                    usbControllers.add(key);
                }
                continue;
            } else if (key.isEmpty()) {
                // Ignore everything preceding the first node
                continue;
            }
            // We are currently processing for node identified by key. Save
            // approrpriate variables to maps.
            line = line.trim();
            if (line.startsWith("model:")) {
                nameMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
            } else if (line.startsWith("name:")) {
                // Name is backup for model if model doesn't exist, so only
                // put if key doesn't yet exist
                nameMap.putIfAbsent(key, ParseUtil.getSingleQuoteStringValue(line));
            } else if (line.startsWith("vendor-id:")) {
                // Format: vendor-id: 00008086
                if (line.length() > 4) {
                    vendorIdMap.put(key, line.substring(line.length() - 4));
                }
            } else if (line.startsWith("device-id:")) {
                // Format: device-id: 00002440
                if (line.length() > 4) {
                    productIdMap.put(key, line.substring(line.length() - 4));
                }
            } else if (line.startsWith("device_type:")) {
                // Name is backup for model if model doesn't exist, so only
                // put if key doesn't yet exist
                deviceTypeMap.putIfAbsent(key, ParseUtil.getSingleQuoteStringValue(line));
            }
        }

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            // Only do controllers that are USB device type
            if ("usb".equals(deviceTypeMap.getOrDefault(controller, ""))) {
                controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000"));
            }
        }
        return controllerDevices.toArray(new UsbDevice[controllerDevices.size()]);
    }

    /**
     * Recursively creates SolarisUsbDevices by fetching information from maps
     * to populate fields
     *
     * @param devPath
     *            The device node path.
     * @param vid
     *            The default (parent) vendor ID
     * @param pid
     *            The default (parent) product ID
     * @return A SolarisUsbDevice corresponding to this device
     */
    private static SolarisUsbDevice getDeviceAndChildren(String devPath, String vid, String pid) {
        String vendorId = vendorIdMap.getOrDefault(devPath, vid);
        String productId = productIdMap.getOrDefault(devPath, pid);
        List<String> childPaths = hubMap.getOrDefault(devPath, new ArrayList<String>());
        List<SolarisUsbDevice> usbDevices = new ArrayList<>();
        for (String path : childPaths) {
            usbDevices.add(getDeviceAndChildren(path, vendorId, productId));
        }
        Collections.sort(usbDevices);
        return new SolarisUsbDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId), "", vendorId, productId,
                "", usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }
}
