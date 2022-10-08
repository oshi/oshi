/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.UsbDevice;
import oshi.hardware.common.AbstractUsbDevice;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * AIX Usb Device
 */
@Immutable
public class AixUsbDevice extends AbstractUsbDevice {

    public AixUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Instantiates a list of {@link oshi.hardware.UsbDevice} objects, representing devices connected via a usb port
     * (including internal devices).
     * <p>
     * If the value of {@code tree} is true, the top level devices returned from this method are the USB Controllers;
     * connected hubs and devices in its device tree share that controller's bandwidth. If the value of {@code tree} is
     * false, USB devices (not controllers) are listed in a single flat list.
     *
     * @param tree  If true, returns a list of controllers, which requires recursive iteration of connected devices. If
     *              false, returns a flat list of devices excluding controllers.
     * @param lscfg A memoized lscfg list
     * @return a list of {@link oshi.hardware.UsbDevice} objects.
     */
    public static List<UsbDevice> getUsbDevices(boolean tree, Supplier<List<String>> lscfg) {
        List<UsbDevice> deviceList = new ArrayList<>();
        for (String line : lscfg.get()) {
            String s = line.trim();
            if (s.startsWith("usb")) {
                String[] split = ParseUtil.whitespaces.split(s, 3);
                if (split.length == 3) {
                    deviceList.add(new AixUsbDevice(split[2], Constants.UNKNOWN, Constants.UNKNOWN, Constants.UNKNOWN,
                            Constants.UNKNOWN, split[0], Collections.emptyList()));
                }
            }
        }
        if (tree) {
            return Arrays.asList(new AixUsbDevice("USB Controller", "", "0000", "0000", "", "", deviceList));
        }
        return deviceList;
    }
}
