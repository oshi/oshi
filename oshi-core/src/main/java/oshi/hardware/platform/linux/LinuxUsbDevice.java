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
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.hardware.platform.mac.MacUsbDevice;
import oshi.jna.platform.linux.Udev;
import oshi.jna.platform.linux.Udev.UdevDevice;
import oshi.jna.platform.linux.Udev.UdevEnumerate;
import oshi.jna.platform.linux.Udev.UdevListEntry;

public class LinuxUsbDevice extends AbstractUsbDevice {

    private static final long serialVersionUID = 2L;

    /*
     * Maps to store information using device node path as the key
     */
    private static Map<String, String> nameMap = new HashMap<>();
    private static Map<String, String> vendorMap = new HashMap<>();
    private static Map<String, String> vendorIdMap = new HashMap<>();
    private static Map<String, String> productIdMap = new HashMap<>();
    private static Map<String, String> serialMap = new HashMap<>();
    private static Map<String, List<String>> hubMap = new HashMap<>();

    public LinuxUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
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
            deviceList.add(new LinuxUsbDevice(device.getName(), device.getVendor(), device.getVendorId(),
                    device.getProductId(), device.getSerialNumber(), new MacUsbDevice[0]));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList.toArray(new UsbDevice[0]);
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, UsbDevice[] connectedDevices) {
        for (UsbDevice device : connectedDevices) {
            deviceList.add(device);
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private static UsbDevice[] getUsbDevices() {
        // Enumerate all usb devices and build information maps
        Udev.UdevHandle udev = Udev.INSTANCE.udev_new();
        // Create a list of the devices in the 'usb' subsystem.
        UdevEnumerate enumerate = Udev.INSTANCE.udev_enumerate_new(udev);
        Udev.INSTANCE.udev_enumerate_add_match_subsystem(enumerate, "usb");
        Udev.INSTANCE.udev_enumerate_scan_devices(enumerate);
        UdevListEntry devices = Udev.INSTANCE.udev_enumerate_get_list_entry(enumerate);

        // Build a list of devices with no parent; these will be the roots
        List<String> usbControllers = new ArrayList<>();
        // Empty out maps
        nameMap.clear();
        vendorMap.clear();
        vendorIdMap.clear();
        productIdMap.clear();
        serialMap.clear();
        hubMap.clear();

        // For each item enumerated, store information in the maps
        for (UdevListEntry dev_list_entry = devices; dev_list_entry != null; dev_list_entry = Udev.INSTANCE
                .udev_list_entry_get_next(dev_list_entry)) {

            // Get the filename of the /sys entry for the device and create a
            // udev_device object (dev) representing it
            String path = Udev.INSTANCE.udev_list_entry_get_name(dev_list_entry);
            UdevDevice dev = Udev.INSTANCE.udev_device_new_from_syspath(udev, path);
            // Ignore interfaces
            if (!"usb_device".equals(Udev.INSTANCE.udev_device_get_devtype(dev))) {
                continue;
            }

            // Use the path as the key for the maps
            String value = Udev.INSTANCE.udev_device_get_sysattr_value(dev, "product");
            if (value != null) {
                nameMap.put(path, value);
            }
            value = Udev.INSTANCE.udev_device_get_sysattr_value(dev, "manufacturer");
            if (value != null) {
                vendorMap.put(path, value);
            }
            value = Udev.INSTANCE.udev_device_get_sysattr_value(dev, "idVendor");
            if (value != null) {
                vendorIdMap.put(path, value);
            }
            value = Udev.INSTANCE.udev_device_get_sysattr_value(dev, "idProduct");
            if (value != null) {
                productIdMap.put(path, value);
            }
            value = Udev.INSTANCE.udev_device_get_sysattr_value(dev, "serial");
            if (value != null) {
                serialMap.put(path, value);
            }
            UdevDevice parent = Udev.INSTANCE.udev_device_get_parent_with_subsystem_devtype(dev, "usb", "usb_device");
            if (parent == null) {
                // This is a controller with no parent, add to list
                usbControllers.add(path);
            } else {
                // Add child path (path variable) to parent's path
                String parentPath = Udev.INSTANCE.udev_device_get_syspath(parent);
                hubMap.computeIfAbsent(parentPath, x -> new ArrayList<>()).add(path);
            }
            Udev.INSTANCE.udev_device_unref(dev);
        }
        // Free the enumerator object
        Udev.INSTANCE.udev_enumerate_unref(enumerate);
        Udev.INSTANCE.udev_unref(udev);

        // Build tree and return
        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000"));
        }
        return controllerDevices.toArray(new UsbDevice[0]);
    }

    /**
     * Recursively creates LinuxUsbDevices by fetching information from maps to
     * populate fields
     *
     * @param devPath
     *            The device node path.
     * @param vid
     *            The default (parent) vendor ID
     * @param pid
     *            The default (parent) product ID
     * @return A LinuxUsbDevice corresponding to this device
     */
    private static LinuxUsbDevice getDeviceAndChildren(String devPath, String vid, String pid) {
        String vendorId = vendorIdMap.getOrDefault(devPath, vid);
        String productId = productIdMap.getOrDefault(devPath, pid);
        List<String> childPaths = hubMap.getOrDefault(devPath, new ArrayList<String>());
        List<LinuxUsbDevice> usbDevices = new ArrayList<>();
        for (String path : childPaths) {
            usbDevices.add(getDeviceAndChildren(path, vendorId, productId));
        }
        Collections.sort(usbDevices);
        return new LinuxUsbDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId),
                vendorMap.getOrDefault(devPath, ""), vendorId, productId, serialMap.getOrDefault(devPath, ""),
                usbDevices.toArray(new UsbDevice[usbDevices.size()]));
    }
}
