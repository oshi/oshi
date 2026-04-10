/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class AbstractUsbDeviceTest {

    @Test
    void testGetters() {
        AbstractUsbDevice device = new AbstractUsbDevice("Mouse", "Logitech", "046d", "c077", "SN1", "USB1",
                Collections.emptyList()) {
        };
        assertThat(device.getName(), is("Mouse"));
        assertThat(device.getVendor(), is("Logitech"));
        assertThat(device.getVendorId(), is("046d"));
        assertThat(device.getProductId(), is("c077"));
        assertThat(device.getSerialNumber(), is("SN1"));
        assertThat(device.getUniqueDeviceId(), is("USB1"));
        assertThat(device.getConnectedDevices(), is(Collections.emptyList()));
    }

    @Test
    void testConnectedDevices() {
        AbstractUsbDevice child = new AbstractUsbDevice("Mouse", "Logitech", "", "", "", "", Collections.emptyList()) {
        };
        AbstractUsbDevice parent = new AbstractUsbDevice("Hub", "Generic", "", "", "", "",
                Collections.singletonList(child)) {
        };
        assertThat(parent.getConnectedDevices(), hasSize(1));
        assertThat(parent.getConnectedDevices().get(0).getName(), is("Mouse"));
    }

    @Test
    void testCompareTo() {
        AbstractUsbDevice a = new AbstractUsbDevice("Alpha", "", "", "", "", "", Collections.emptyList()) {
        };
        AbstractUsbDevice b = new AbstractUsbDevice("Beta", "", "", "", "", "", Collections.emptyList()) {
        };
        assertThat(a.compareTo(b), is(lessThan(0)));
    }

    @Test
    void testToStringWithChildren() {
        AbstractUsbDevice child = new AbstractUsbDevice("Mouse", "Logitech", "", "", "SN1", "",
                Collections.emptyList()) {
        };
        AbstractUsbDevice parent = new AbstractUsbDevice("Hub", "", "", "", "", "", Collections.singletonList(child)) {
        };
        assertThat(parent.toString(), is(" Hub\n |-- Mouse (Logitech) [s/n: SN1]"));
    }
}
