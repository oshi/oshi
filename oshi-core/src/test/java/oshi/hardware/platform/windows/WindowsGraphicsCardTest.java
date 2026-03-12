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
        assertThat(WindowsGraphicsCard.parsePciBusNumber("PCI bus 1, device 0, function 0"), is(1));
    }

    @Test
    void testParsePciBusNumberBusZero() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber("PCI bus 0, device 0, function 0"), is(0));
    }

    @Test
    void testParsePciBusNumberHighBusNumber() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber("PCI bus 255, device 31, function 7"), is(255));
    }

    @Test
    void testParsePciBusNumberCaseInsensitive() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber("pci bus 3, device 0, function 0"), is(3));
    }

    @Test
    void testParsePciBusNumberNull() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber(null), is(-1));
    }

    @Test
    void testParsePciBusNumberEmpty() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber(""), is(-1));
    }

    @Test
    void testParsePciBusNumberUnrecognizedFormat() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber("Bus 1, Device 0"), is(-1));
    }

    @Test
    void testParsePciBusNumberNonNumericAfterPrefix() {
        assertThat(WindowsGraphicsCard.parsePciBusNumber("PCI bus X, device 0, function 0"), is(-1));
    }
}
