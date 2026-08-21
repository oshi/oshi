/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;

class DisplayConnectorTest {

    @Test
    void testConnectorNameSingleConnectorHasNoIndex() {
        // connectorInstance 0 means the adapter has a single connector of that type
        assertThat(DisplayConnector.connectorName(5, 0), is("HDMI"));
        assertThat(DisplayConnector.connectorName(10, 0), is("DisplayPort"));
        assertThat(DisplayConnector.connectorName(11, 0), is("eDP"));
        assertThat(DisplayConnector.connectorName(0x80000000, 0), is("Internal"));
    }

    @Test
    void testConnectorNameMultipleConnectorsAppendInstance() {
        assertThat(DisplayConnector.connectorName(5, 1), is("HDMI-1"));
        assertThat(DisplayConnector.connectorName(10, 2), is("DisplayPort-2"));
    }

    @Test
    void testConnectorNameKnownTechnologies() {
        assertThat(DisplayConnector.connectorName(0, 0), is("VGA"));
        assertThat(DisplayConnector.connectorName(4, 0), is("DVI"));
        assertThat(DisplayConnector.connectorName(6, 0), is("LVDS"));
    }

    @Test
    void testConnectorNameUnknownTechnology() {
        assertThat(DisplayConnector.connectorName(-1, 0), is("Other"));
        assertThat(DisplayConnector.connectorName(999, 0), is("Other"));
    }

    @Test
    void testNormalizePathLowerCases() {
        assertThat(DisplayConnector.normalizePath("\\\\?\\DISPLAY#DELA1CD#5&2a1c8d3&0&UID4353#{E6F07B5F}"),
                is("\\\\?\\display#dela1cd#5&2a1c8d3&0&uid4353#{e6f07b5f}"));
    }

    @Test
    void testNormalizePathBlankIsUnknown() {
        assertThat(DisplayConnector.normalizePath(""), is(Constants.UNKNOWN));
    }
}
