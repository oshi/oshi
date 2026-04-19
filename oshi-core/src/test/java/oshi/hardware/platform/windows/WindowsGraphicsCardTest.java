/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.WINDOWS)
class WindowsGraphicsCardTest {

    @Test
    void testParsePciBusNumberTypicalFormat() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber("PCI bus 1, device 0, function 0"), is(1));
    }

    @Test
    void testParsePciBusNumberBusZero() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber("PCI bus 0, device 0, function 0"), is(0));
    }

    @Test
    void testParsePciBusNumberHighBusNumber() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber("PCI bus 255, device 31, function 7"), is(255));
    }

    @Test
    void testParsePciBusNumberCaseInsensitive() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber("pci bus 3, device 0, function 0"), is(3));
    }

    @Test
    void testParsePciBusNumberNull() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber(null), is(-1));
    }

    @Test
    void testParsePciBusNumberEmpty() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber(""), is(-1));
    }

    @Test
    void testParsePciBusNumberUnrecognizedFormat() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber("Bus 1, Device 0"), is(-1));
    }

    @Test
    void testParsePciBusNumberNonNumericAfterPrefix() {
        assertThat(WindowsGraphicsCardJNA.parsePciBusNumber("PCI bus X, device 0, function 0"), is(-1));
    }

    @Test
    void testParsePciDevice() {
        assertThat(WindowsGraphicsCardJNA.parsePciDevice("PCI bus 1, device 2, function 3"), is(2));
    }

    @Test
    void testParsePciDeviceNull() {
        assertThat(WindowsGraphicsCardJNA.parsePciDevice(null), is(-1));
    }

    @Test
    void testParsePciFunction() {
        assertThat(WindowsGraphicsCardJNA.parsePciFunction("PCI bus 1, device 2, function 3"), is(3));
    }

    @Test
    void testParsePciFunctionNull() {
        assertThat(WindowsGraphicsCardJNA.parsePciFunction(null), is(-1));
    }

    @Test
    void testBuildPciBusId() {
        assertThat(WindowsGraphicsCardJNA.buildPciBusId("PCI bus 1, device 0, function 0"), is("0000:01:00.0"));
    }

    @Test
    void testBuildPciBusIdHighValues() {
        assertThat(WindowsGraphicsCardJNA.buildPciBusId("PCI bus 255, device 31, function 7"), is("0000:ff:1f.7"));
    }

    @Test
    void testBuildPciBusIdUnparseable() {
        assertThat(WindowsGraphicsCardJNA.buildPciBusId("not a pci string"), is(""));
    }

    @Test
    void testBuildPciBusIdNull() {
        assertThat(WindowsGraphicsCardJNA.buildPciBusId(null), is(""));
    }
}
