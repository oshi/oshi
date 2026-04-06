/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;

/**
 * Linux USB device base class. Subclasses implement udev enumeration via JNA or FFM.
 */
@Immutable
public abstract class LinuxUsbDevice extends AbstractUsbDevice {

    protected static final String SUBSYSTEM_USB = "usb";
    protected static final String DEVTYPE_USB_DEVICE = "usb_device";
    protected static final String ATTR_PRODUCT = "product";
    protected static final String ATTR_MANUFACTURER = "manufacturer";
    protected static final String ATTR_VENDOR_ID = "idVendor";
    protected static final String ATTR_PRODUCT_ID = "idProduct";
    protected static final String ATTR_SERIAL = "serial";

    protected LinuxUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Enumerates USB devices via the platform-specific udev binding and populates the provided maps.
     *
     * @param usbControllers list to add root controller syspaths to
     * @param nameMap        map of syspath to product name
     * @param vendorMap      map of syspath to vendor/manufacturer name
     * @param vendorIdMap    map of syspath to vendor ID
     * @param productIdMap   map of syspath to product ID
     * @param serialMap      map of syspath to serial number
     * @param hubMap         map of parent syspath to list of child syspaths
     */
    protected abstract void enumerateUsbDevices(List<String> usbControllers, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap);

    /**
     * Creates a new instance of this concrete subclass.
     *
     * @param name             the device name
     * @param vendor           the vendor name
     * @param vendorId         the vendor ID
     * @param productId        the product ID
     * @param serialNumber     the serial number
     * @param uniqueDeviceId   the unique device ID (syspath)
     * @param connectedDevices the list of connected child devices
     * @return a new instance of the concrete subclass
     */
    protected abstract LinuxUsbDevice createDevice(String name, String vendor, String vendorId, String productId,
            String serialNumber, String uniqueDeviceId, List<UsbDevice> connectedDevices);

    /**
     * Instantiates a list of {@link oshi.hardware.UsbDevice} objects, representing devices connected via a USB port
     * (including internal devices).
     * <p>
     * If the value of {@code tree} is true, the top level devices returned from this method are the USB Controllers;
     * connected hubs and devices in its device tree share that controller's bandwidth. If the value of {@code tree} is
     * false, all devices (including controllers) are listed in a single flat list with no nested connectedDevices.
     *
     * @param tree If true, returns a list of controllers with their device tree. If false, returns a flat list of all
     *             devices (including controllers) with no nested connectedDevices.
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    protected List<UsbDevice> queryUsbDevices(boolean tree) {
        List<String> usbControllers = new ArrayList<>();
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        enumerateUsbDevices(usbControllers, nameMap, vendorMap, vendorIdMap, productIdMap, serialMap, hubMap);

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }

        if (tree) {
            return controllerDevices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        for (UsbDevice device : controllerDevices) {
            deviceList
                    .add(createDevice(device.getName(), device.getVendor(), device.getVendorId(), device.getProductId(),
                            device.getSerialNumber(), device.getUniqueDeviceId(), Collections.emptyList()));
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    private static void addDevicesToList(List<UsbDevice> deviceList, List<UsbDevice> list) {
        for (UsbDevice device : list) {
            deviceList.add(device);
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
    }

    private LinuxUsbDevice getDeviceAndChildren(String devPath, String vid, String pid, Map<String, String> nameMap,
            Map<String, String> vendorMap, Map<String, String> vendorIdMap, Map<String, String> productIdMap,
            Map<String, String> serialMap, Map<String, List<String>> hubMap) {
        String vendorId = vendorIdMap.getOrDefault(devPath, vid);
        String productId = productIdMap.getOrDefault(devPath, pid);
        List<UsbDevice> usbDevices = new ArrayList<>();
        for (String path : hubMap.getOrDefault(devPath, Collections.emptyList())) {
            usbDevices.add(getDeviceAndChildren(path, vendorId, productId, nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        Collections.sort(usbDevices);
        return createDevice(nameMap.getOrDefault(devPath, vendorId + ":" + productId),
                vendorMap.getOrDefault(devPath, ""), vendorId, productId, serialMap.getOrDefault(devPath, ""), devPath,
                usbDevices);
    }
}
