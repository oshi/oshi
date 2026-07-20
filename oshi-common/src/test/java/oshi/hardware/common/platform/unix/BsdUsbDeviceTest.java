/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.UsbDevice;

class BsdUsbDeviceTest {

    // Representative usbdevs -v output from OpenBSD/NetBSD
    // Format: "addr NN: VVVV:PPPP VendorName, ProductName" (colon at pos 7, comma separates vendor from product)
    private static final List<String> USBDEVS = Arrays.asList(//
            "Controller /dev/usb0:", //
            "addr 01: 0000:0000 UHCI, root hub", //
            "  iSerial 12345", //
            "addr 02: 8087:0024 Intel, Rate Matching Hub", //
            "addr 03: 046d:c52b Logitech, Unifying Receiver");

    @Test
    void testParseUsbDevices() {
        List<UsbDevice> devices = BsdUsbDevice.parseUsbDevices(USBDEVS);
        // Should have one root hub controller
        assertThat(devices, hasSize(1));
        UsbDevice root = devices.get(0);
        assertThat(root.getName(), is("root hub"));
        assertThat(root.getSerialNumber(), is("12345"));
        assertThat(root.getVendorId(), is("0000"));
        assertThat(root.getProductId(), is("0000"));
    }

    @Test
    void testParseUsbDevicesMultipleControllers() {
        List<String> usbdevs = Arrays.asList(//
                "Controller /dev/usb0:", //
                "addr 01: 0000:0000 UHCI, root hub 0", //
                "Controller /dev/usb1:", //
                "addr 01: 0000:0000 EHCI, root hub 1");
        List<UsbDevice> devices = BsdUsbDevice.parseUsbDevices(usbdevs);
        assertThat(devices, hasSize(2));
        assertThat(devices.get(0).getName(), is("root hub 0"));
        assertThat(devices.get(1).getName(), is("root hub 1"));
    }

    @Test
    void testParseUsbDevicesEmpty() {
        List<UsbDevice> devices = BsdUsbDevice.parseUsbDevices(Collections.emptyList());
        assertThat(devices, is(empty()));
    }
}
