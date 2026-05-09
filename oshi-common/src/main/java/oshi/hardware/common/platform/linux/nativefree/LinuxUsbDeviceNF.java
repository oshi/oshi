/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.UsbDevice;
import oshi.hardware.common.platform.linux.LinuxUsbDevice;
import oshi.util.FileUtil;
import oshi.util.linux.SysPath;

/**
 * Native-free Linux USB device enumeration from {@code /sys/bus/usb/devices/}.
 */
public final class LinuxUsbDeviceNF extends LinuxUsbDevice {

    private static final String SYS_USB = SysPath.SYS + "bus/usb/devices/";

    private LinuxUsbDeviceNF() {
    }

    /**
     * Gets USB devices on this machine.
     *
     * @param tree if true, returns controllers with device tree; if false, flat list excluding controllers
     * @return a list of {@link UsbDevice} objects
     */
    public static List<UsbDevice> getUsbDevices(boolean tree) {
        List<UsbDevice> devices = queryUsbDevices(SYS_USB);
        if (tree) {
            return devices;
        }
        List<UsbDevice> deviceList = new ArrayList<>();
        for (UsbDevice device : devices) {
            addDevicesToList(deviceList, device.getConnectedDevices());
        }
        return deviceList;
    }

    /**
     * Queries USB devices from the specified sysfs path.
     *
     * @param sysUsbPath the path to the USB devices directory (e.g., /sys/bus/usb/devices/)
     * @return a list of USB device trees rooted at controllers
     */
    static List<UsbDevice> queryUsbDevices(String sysUsbPath) {
        if (!sysUsbPath.endsWith(File.separator)) {
            sysUsbPath += File.separator;
        }
        File usbDir = new File(sysUsbPath);
        File[] deviceDirs = usbDir.listFiles();
        if (deviceDirs == null) {
            return new ArrayList<>();
        }

        List<String> usbControllers = new ArrayList<>();
        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> vendorMap = new HashMap<>();
        Map<String, String> vendorIdMap = new HashMap<>();
        Map<String, String> productIdMap = new HashMap<>();
        Map<String, String> serialMap = new HashMap<>();
        Map<String, List<String>> hubMap = new HashMap<>();

        for (File devDir : deviceDirs) {
            String syspath = devDir.getAbsolutePath();
            String devtype = FileUtil.getStringFromFile(syspath + "/devnum").trim();
            if (devtype.isEmpty()) {
                continue;
            }

            String product = FileUtil.getStringFromFile(syspath + "/product").trim();
            if (!product.isEmpty()) {
                nameMap.put(syspath, product);
            }
            String manufacturer = FileUtil.getStringFromFile(syspath + "/manufacturer").trim();
            if (!manufacturer.isEmpty()) {
                vendorMap.put(syspath, manufacturer);
            }
            String idVendor = FileUtil.getStringFromFile(syspath + "/idVendor").trim();
            if (!idVendor.isEmpty()) {
                vendorIdMap.put(syspath, idVendor);
            }
            String idProduct = FileUtil.getStringFromFile(syspath + "/idProduct").trim();
            if (!idProduct.isEmpty()) {
                productIdMap.put(syspath, idProduct);
            }
            String serial = FileUtil.getStringFromFile(syspath + "/serial").trim();
            if (!serial.isEmpty()) {
                serialMap.put(syspath, serial);
            }

            // Determine parent: USB root hubs have names like "usbN", children have "N-N.N..."
            String name = devDir.getName();
            if (name.startsWith("usb")) {
                usbControllers.add(syspath);
            } else {
                String parentName;
                if (name.contains(".")) {
                    parentName = name.substring(0, name.lastIndexOf('.'));
                } else if (name.contains("-")) {
                    parentName = "usb" + name.substring(0, name.indexOf('-'));
                } else {
                    parentName = name;
                }
                String parentPath = sysUsbPath + parentName;
                hubMap.computeIfAbsent(parentPath, x -> new ArrayList<>()).add(syspath);
            }
        }

        List<UsbDevice> controllerDevices = new ArrayList<>();
        for (String controller : usbControllers) {
            controllerDevices.add(getDeviceAndChildren(controller, "0000", "0000", nameMap, vendorMap, vendorIdMap,
                    productIdMap, serialMap, hubMap));
        }
        return controllerDevices;
    }
}
