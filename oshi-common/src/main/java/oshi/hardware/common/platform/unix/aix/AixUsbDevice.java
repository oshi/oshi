/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

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
 * AIX Usb Device. Builds a flat single-controller list directly from {@code lscfg} output; this intentionally does not
 * use the shared {@code AbstractUsbDevice.buildDeviceTree}, as {@code lscfg} exposes no parent/child device tree.
 */
@Immutable
public class AixUsbDevice extends AbstractUsbDevice {

    public AixUsbDevice(String name, String vendor, String vendorId, String productId, String serialNumber,
            String uniqueDeviceId, List<UsbDevice> connectedDevices) {
        super(name, vendor, vendorId, productId, serialNumber, uniqueDeviceId, connectedDevices);
    }

    /**
     * Instantiates the USB controller device tree. The flat form is derived by the caller (the HAL).
     *
     * @param lscfg A memoized lscfg list
     * @return a list containing the USB controller with its connected devices.
     */
    public static List<UsbDevice> getUsbDevices(Supplier<List<String>> lscfg) {
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
        return Arrays.asList(new AixUsbDevice("USB Controller", "", "0000", "0000", "", "", deviceList));
    }
}
