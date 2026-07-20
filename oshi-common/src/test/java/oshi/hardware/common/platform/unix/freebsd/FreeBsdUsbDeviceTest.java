/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.UsbDevice;

class FreeBsdUsbDeviceTest {

    // A minimal but real-shaped lshal tree: a host controller, its usbus, and one device beneath the usbus.
    // getUsbDevices() promotes the usbus's children onto the controller and drops the usbus node itself.
    private static final List<String> LSHAL = Arrays.asList(//
            "udi = '/org/freedesktop/Hal/devices/pci_1022_43d5'", //
            "  info.product = 'xHCI Host Controller'  (string)", //
            "udi = '/org/freedesktop/Hal/devices/usb_device_0_0_usbus0'", //
            "  freebsd.driver = 'usbus'  (string)", //
            "  info.parent = '/org/freedesktop/Hal/devices/pci_1022_43d5'  (string)", //
            "udi = '/org/freedesktop/Hal/devices/usb_device_781_5567'", //
            "  info.parent = '/org/freedesktop/Hal/devices/usb_device_0_0_usbus0'  (string)", //
            "  usb_device.product = 'Ultra USB 3.0'  (string)", //
            "  usb_device.vendor = 'SanDisk'  (string)", //
            "  usb_device.vendor_id = 1921  (0x781)  (int)", //
            "  usb_device.product_id = 21863  (0x5567)  (int)", //
            "  usb_device.serial = '4C530001120510117433'  (string)");

    @Test
    void testParseUsbDevices() {
        List<UsbDevice> controllers = FreeBsdUsbDevice.parseUsbDevices(LSHAL);
        assertThat(controllers, hasSize(1));

        UsbDevice controller = controllers.get(0);
        assertThat(controller.getName(), is("xHCI Host Controller"));
        assertThat(controller.getUniqueDeviceId(), is("/org/freedesktop/Hal/devices/pci_1022_43d5"));
        // The usbus node is skipped; its child is promoted directly onto the controller.
        assertThat(controller.getConnectedDevices(), hasSize(1));

        UsbDevice device = controller.getConnectedDevices().get(0);
        assertThat(device.getName(), is("Ultra USB 3.0"));
        assertThat(device.getVendor(), is("SanDisk"));
        assertThat(device.getVendorId(), is("0781"));
        assertThat(device.getProductId(), is("5567"));
        assertThat(device.getSerialNumber(), is("4C530001120510117433"));
    }

    @Test
    void testParseUsbDevicesHexSerial() {
        // A 0x-prefixed serial is decoded from its hex byte string.
        List<String> lshal = Arrays.asList(//
                "udi = '/org/freedesktop/Hal/devices/pci_0'", //
                "  info.product = 'Root Hub'  (string)", //
                "udi = '/org/freedesktop/Hal/devices/usbus0'", //
                "  freebsd.driver = 'usbus'  (string)", //
                "  info.parent = '/org/freedesktop/Hal/devices/pci_0'  (string)", //
                "udi = '/org/freedesktop/Hal/devices/dev0'", //
                "  info.parent = '/org/freedesktop/Hal/devices/usbus0'  (string)", //
                "  usb_device.serial = '0x41424344'  (string)");
        List<UsbDevice> controllers = FreeBsdUsbDevice.parseUsbDevices(lshal);
        assertThat(controllers, hasSize(1));
        assertThat(controllers.get(0).getConnectedDevices(), hasSize(1));
        // 0x41424344 -> "ABCD"
        assertThat(controllers.get(0).getConnectedDevices().get(0).getSerialNumber(), is("ABCD"));
    }

    @Test
    void testParseUsbDevicesSkipsInterfaceNodes() {
        // An interface node's udi is "<device-udi>_ifN"; its .parent points at the device, so key.replace(parent)
        // begins with "_if" and the node must be skipped rather than treated as a child device.
        List<String> lshal = Arrays.asList(//
                "udi = '/dev/pci0'", //
                "  info.product = 'Root Hub'  (string)", //
                "udi = '/dev/usbus0'", //
                "  freebsd.driver = 'usbus'  (string)", //
                "  info.parent = '/dev/pci0'  (string)", //
                "udi = '/dev/dev0'", //
                "  info.parent = '/dev/usbus0'  (string)", //
                "  usb_device.product = 'Widget'  (string)", //
                "udi = '/dev/dev0_if0'", //
                "  info.parent = '/dev/dev0'  (string)", //
                "  usb.interface.number = 0  (int)");
        List<UsbDevice> controllers = FreeBsdUsbDevice.parseUsbDevices(lshal);
        assertThat(controllers, hasSize(1));
        // Only the real device hangs off the controller; the _if interface node is not a child.
        assertThat(controllers.get(0).getConnectedDevices(), hasSize(1));
        assertThat(controllers.get(0).getConnectedDevices().get(0).getName(), is("Widget"));
    }

    @Test
    void testParseUsbDevicesEmpty() {
        assertThat(FreeBsdUsbDevice.parseUsbDevices(Collections.emptyList()), is(empty()));
    }
}
