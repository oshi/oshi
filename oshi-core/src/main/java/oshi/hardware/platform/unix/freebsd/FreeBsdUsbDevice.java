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
package oshi.hardware.platform.unix.freebsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public class FreeBsdUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    /*
     * Maps to store information using node # as the key
     */
    private static Map<String, String> nameMap = new HashMap<>();
    private static Map<String, String> vendorMap = new HashMap<>();
    private static Map<String, String> vendorIdMap = new HashMap<>();
    private static Map<String, String> productIdMap = new HashMap<>();
    private static Map<String, String> serialMap = new HashMap<>();
    private static Map<String, String> parentMap = new HashMap<>();
    private static Map<String, List<String>> hubMap = new HashMap<>();

    public FreeBsdUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
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
            deviceList.add(new FreeBsdUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), new FreeBsdUsbDevice[0]));
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
        // Empty out maps
        nameMap.clear();
        vendorMap.clear();
        vendorIdMap.clear();
        productIdMap.clear();
        serialMap.clear();
        hubMap.clear();
        parentMap.clear();

        // Enumerate all devices and build information maps. This will build the
        // entire device tree; we will identify the controllers as the parents
        // of the usbus entries and eventually only populate the returned
        // results with those
        List<String> devices = ExecutingCommand.runNative("lshal");
        if (devices.isEmpty()) {
            // TODO usbconfig, works as root
            return new FreeBsdUsbDevice[0];
        }
        // For each item enumerated, store information in the maps
        String key = "";
        List<String> usBuses = new ArrayList<>();
        for (String line : devices) {
            // udi = ... identifies start of a new tree
            if (line.startsWith("udi =")) {
                // Remove indent for key
                key = ParseUtil.getSingleQuoteStringValue(line);
                continue;
            } else if (key.isEmpty()) {
                // Ignore everything preceding the first node
                continue;
            }
            // We are currently processing for node identified by key. Save
            // approrpriate variables to maps.
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            } else if (line.startsWith("freebsd.driver =")
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
            } else if (line.contains(".vendor =")) {
                vendorMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
            } else if (line.contains(".product =")) {
                nameMap.put(key, ParseUtil.getSingleQuoteStringValue(line));
            } else if (line.contains(".serial =")) {
                String serial = ParseUtil.getSingleQuoteStringValue(line);
                serialMap.put(key,
                        serial.startsWith("0x") ? ParseUtil.hexStringToString(serial.replace("0x", "")) : serial);
            } else if (line.contains(".vendor_id =") || line.contains(".product_id =")) {
                vendorIdMap.put(key, String.format("%04x", ParseUtil.getFirstIntValue(line)));
            }
        }

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String usbus : usBuses) {
            // Skip the usbuses: make their parents the controllers and replace
            // parents' children with the buses' children
            String parent = parentMap.get(usbus);
            hubMap.put(parent, hubMap.get(usbus));
            controllerDevices.add(getDeviceAndChildren(parent, "0000", "0000"));
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
    private static FreeBsdUsbDevice getDeviceAndChildren(String devPath, String vid, String pid) {
        String vendorId = vendorIdMap.getOrDefault(devPath, vid);
        String productId = productIdMap.getOrDefault(devPath, pid);
        List<String> childPaths = hubMap.getOrDefault(devPath, new ArrayList<String>());
        List<FreeBsdUsbDevice> usbDevices = new ArrayList<>();
        for (String path : childPaths) {
            usbDevices.add(getDeviceAndChildren(path, vendorId, productId));
        }
        Collections.sort(usbDevices);
        return new FreeBsdUsbDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId), "", vendorId, productId,
                "", usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }
}
