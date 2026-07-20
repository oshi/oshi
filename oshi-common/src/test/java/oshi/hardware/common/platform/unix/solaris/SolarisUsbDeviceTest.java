/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.UsbDevice;

class SolarisUsbDeviceTest {

    @Test
    void testParsePrtconfEmpty() {
        List<UsbDevice> devices = SolarisUsbDevice.parsePrtconf(Collections.emptyList());
        assertThat(devices, is(empty()));
    }

    @Test
    void testParsePrtconfUsbController() {
        // A USB controller (class-code 000c) with one child device
        List<String> prtconf = Arrays.asList("    Node 0xf0a8b0", //
                "      name:  'usb-controller'", //
                "      model:  'EHCI Host Controller'", //
                "      vendor-id:  00008086", //
                "      device-id:  00002934", //
                "      class-code:  000c0320", //
                "        Node 0xf0a900", //
                "          name:  'storage'", //
                "          model:  'USB Flash Drive'", //
                "          vendor-id:  00000781", //
                "          device-id:  00005567");
        List<UsbDevice> devices = SolarisUsbDevice.parsePrtconf(prtconf);
        assertThat(devices, hasSize(1));
        UsbDevice controller = devices.get(0);
        assertThat(controller.getName(), is("EHCI Host Controller"));
        assertThat(controller.getVendor(), is(""));
        assertThat(controller.getVendorId(), is("8086"));
        assertThat(controller.getProductId(), is("2934"));
        // Child device
        assertThat(controller.getConnectedDevices(), hasSize(1));
        UsbDevice child = controller.getConnectedDevices().get(0);
        assertThat(child.getName(), is("USB Flash Drive"));
        assertThat(child.getVendorId(), is("0781"));
        assertThat(child.getProductId(), is("5567"));
    }

    @Test
    void testParsePrtconfDeviceTypeUsb() {
        // A controller identified by device_type 'usb' rather than class-code
        List<String> prtconf = Arrays.asList("    Node 0xaabb00", //
                "      name:  'usb'", //
                "      device_type:  'usb'", //
                "      vendor-id:  00001033", //
                "      device-id:  00000194");
        List<UsbDevice> devices = SolarisUsbDevice.parsePrtconf(prtconf);
        assertThat(devices, hasSize(1));
        assertThat(devices.get(0).getName(), is("usb"));
    }

    @Test
    void testParsePrtconfNonUsbController() {
        // A non-USB controller (class-code 0006 = bridge)
        List<String> prtconf = Arrays.asList("    Node 0xdead00", //
                "      name:  'pci-bridge'", //
                "      vendor-id:  00008086", //
                "      device-id:  0000244e", //
                "      class-code:  00060400");
        List<UsbDevice> devices = SolarisUsbDevice.parsePrtconf(prtconf);
        assertThat(devices, is(empty()));
    }
}
