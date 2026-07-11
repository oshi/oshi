/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import static java.util.Collections.emptyList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oshi.hardware.common.platform.linux.UdevUsbDevice;
import oshi.util.FileUtil;
import oshi.util.linux.SysPath;

/**
 * Native-free Linux USB device enumeration from {@code /sys/bus/usb/devices/}, returning raw {@link UdevUsbDevice}
 * attributes for {@link oshi.hardware.common.platform.linux.LinuxUsbDevice} to assemble into a device tree.
 */
public final class LinuxUsbDeviceNF {

    private static final String SYS_USB = SysPath.SYS + "bus/usb/devices/";

    private LinuxUsbDeviceNF() {
    }

    /**
     * Enumerates USB devices from sysfs.
     *
     * @return the raw USB device attributes
     */
    public static List<UdevUsbDevice> queryUsbDevices() {
        return queryUsbDevices(SYS_USB);
    }

    /**
     * Enumerates USB devices from the specified sysfs path.
     *
     * @param sysUsbPath the path to the USB devices directory (e.g., /sys/bus/usb/devices/)
     * @return the raw USB device attributes
     */
    static List<UdevUsbDevice> queryUsbDevices(String sysUsbPath) {
        if (!sysUsbPath.endsWith(File.separator)) {
            sysUsbPath += File.separator;
        }
        File[] deviceDirs = new File(sysUsbPath).listFiles();
        if (deviceDirs == null) {
            return emptyList();
        }

        List<UdevUsbDevice> devices = new ArrayList<>();
        for (File devDir : deviceDirs) {
            String syspath = devDir.getAbsolutePath();
            if (FileUtil.getStringFromFile(syspath + "/devnum").trim().isEmpty()) {
                continue;
            }
            String product = emptyToNull(FileUtil.getStringFromFile(syspath + "/product").trim());
            String manufacturer = emptyToNull(FileUtil.getStringFromFile(syspath + "/manufacturer").trim());
            String idVendor = emptyToNull(FileUtil.getStringFromFile(syspath + "/idVendor").trim());
            String idProduct = emptyToNull(FileUtil.getStringFromFile(syspath + "/idProduct").trim());
            String serial = emptyToNull(FileUtil.getStringFromFile(syspath + "/serial").trim());

            // Determine the parent from the device name: USB root hubs are "usbN", children are "N-N.N..."
            String name = devDir.getName();
            String parentSyspath;
            if (name.startsWith("usb")) {
                parentSyspath = null;
            } else if (name.contains(".")) {
                parentSyspath = sysUsbPath + name.substring(0, name.lastIndexOf('.'));
            } else if (name.contains("-")) {
                parentSyspath = sysUsbPath + "usb" + name.substring(0, name.indexOf('-'));
            } else {
                parentSyspath = sysUsbPath + name;
            }
            devices.add(new UdevUsbDevice(syspath, product, manufacturer, idVendor, idProduct, serial, parentSyspath));
        }
        return devices;
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
