/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;

class MacDisplayPortTest {

    @Test
    void testFromTransportDescription() {
        assertThat(MacDisplayPort.fromTransportDescription("Port-HDMI@1/DisplayPort"), is("Port-HDMI@1"));
        assertThat(MacDisplayPort.fromTransportDescription("Port-USB-C@2/DisplayPort"), is("Port-USB-C@2"));
    }

    @Test
    void testFromTransportDescriptionNoDelimiter() {
        assertThat(MacDisplayPort.fromTransportDescription("Port-HDMI@1"), is("Port-HDMI@1"));
    }

    @Test
    void testFromTransportDescriptionNullOrBlank() {
        assertThat(MacDisplayPort.fromTransportDescription(null), is(Constants.UNKNOWN));
        assertThat(MacDisplayPort.fromTransportDescription(""), is(Constants.UNKNOWN));
    }

    @Test
    void testFromTransportDescriptionLeadingDelimiter() {
        assertThat(MacDisplayPort.fromTransportDescription("/DisplayPort"), is(Constants.UNKNOWN));
    }

    @Test
    void testFromDeviceTreeName() {
        assertThat(MacDisplayPort.fromDeviceTreeName("disp0,t6030"), is("disp0"));
        assertThat(MacDisplayPort.fromDeviceTreeName("dispext1,t6030"), is("dispext1"));
    }

    @Test
    void testFromDeviceTreeNameNoDelimiter() {
        assertThat(MacDisplayPort.fromDeviceTreeName("disp0"), is("disp0"));
    }

    @Test
    void testFromDeviceTreeNameNullOrBlank() {
        assertThat(MacDisplayPort.fromDeviceTreeName(null), is(Constants.UNKNOWN));
        assertThat(MacDisplayPort.fromDeviceTreeName(""), is(Constants.UNKNOWN));
    }

    @Test
    void testFromDeviceTreeNameLeadingDelimiter() {
        assertThat(MacDisplayPort.fromDeviceTreeName(",t6030"), is(Constants.UNKNOWN));
    }
}
