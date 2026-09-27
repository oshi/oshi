/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.DisplayConnector.Connector;
import oshi.hardware.DisplayMode;
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

    // One DISPLAYCONFIG_PATH_INFO whose source indexes the given mode, rotated and at the given refresh rate
    private static ByteBuffer path(int sourceModeIndex, int rotation, int refreshNumerator, int refreshDenominator) {
        ByteBuffer path = ByteBuffer.allocate(DisplayConnector.PATH_INFO_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        path.putInt(DisplayConnector.PATH_SOURCE_MODE_IDX_OFFSET, sourceModeIndex);
        path.putInt(DisplayConnector.PATH_TARGET_ROTATION_OFFSET, rotation);
        path.putInt(DisplayConnector.PATH_TARGET_REFRESH_NUMERATOR_OFFSET, refreshNumerator);
        path.putInt(DisplayConnector.PATH_TARGET_REFRESH_DENOMINATOR_OFFSET, refreshDenominator);
        return path;
    }

    // A DISPLAYCONFIG_MODE_INFO array: a target mode at index 0, a source mode at index 1
    private static ByteBuffer modes() {
        ByteBuffer modes = ByteBuffer.allocate(2 * DisplayConnector.MODE_INFO_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        // DISPLAYCONFIG_MODE_INFO_TYPE_TARGET
        modes.putInt(0, 2);
        int source = DisplayConnector.MODE_INFO_SIZE;
        // DISPLAYCONFIG_MODE_INFO_TYPE_SOURCE, then width, height, pixelFormat, position.x, position.y
        modes.putInt(source, 1);
        modes.putInt(source + 16, 1080);
        modes.putInt(source + 20, 1920);
        modes.putInt(source + 24, 4);
        modes.putInt(source + 28, -1080);
        modes.putInt(source + 32, -240);
        return modes;
    }

    private static @Nullable DisplayMode readMode(ByteBuffer path, ByteBuffer modes) {
        return DisplayConnector.readMode(off -> path.getInt((int) off), off -> modes.getInt((int) off), 2);
    }

    @Test
    void testReadModeFromSourceModeAndTarget() {
        // DISPLAYCONFIG_ROTATION_ROTATE90, 143.998 Hz as a rational
        DisplayMode mode = readMode(path(1, 2, 143998, 1000), modes());
        assertNotNull(mode);
        assertThat(mode.getX(), is(-1080));
        assertThat(mode.getY(), is(-240));
        assertThat(mode.getWidth(), is(1080));
        assertThat(mode.getHeight(), is(1920));
        assertThat(mode.getPixelWidth(), is(1080));
        assertThat(mode.getPixelHeight(), is(1920));
        assertThat(mode.getRotation(), is(90));
        assertThat(mode.getRefreshRate(), is(143.998));
    }

    @Test
    void testReadModeInvalidIndex() {
        // DISPLAYCONFIG_PATH_MODE_IDX_INVALID
        assertThat(readMode(path(0xffffffff, 1, 60, 1), modes()), is(nullValue()));
        assertThat(readMode(path(2, 1, 60, 1), modes()), is(nullValue()));
    }

    @Test
    void testReadModeIndexingTargetModeIsNull() {
        assertThat(readMode(path(0, 1, 60, 1), modes()), is(nullValue()));
    }

    @Test
    void testRotationDegrees() {
        assertThat(DisplayConnector.rotationDegrees(1), is(0));
        assertThat(DisplayConnector.rotationDegrees(2), is(90));
        assertThat(DisplayConnector.rotationDegrees(3), is(180));
        assertThat(DisplayConnector.rotationDegrees(4), is(270));
        assertThat(DisplayConnector.rotationDegrees(0), is(0));
    }

    @Test
    void testRefreshRate() {
        assertThat(DisplayConnector.refreshRate(60000, 1001), is(60000d / 1001));
        assertThat(DisplayConnector.refreshRate(60, 0), is(0d));
        // The rational's fields are unsigned
        assertThat(DisplayConnector.refreshRate(0x80000000, 0x80000000), is(1d));
    }

    @Test
    void testIsBuiltIn() {
        // LVDS, embedded DisplayPort, embedded UDI, internal
        for (int technology : new int[] { 6, 11, 13, 0x80000000 }) {
            assertThat(DisplayConnector.isBuiltIn(technology), is(true));
        }
        // VGA, HDMI, external DisplayPort, external UDI, Miracast, other
        for (int technology : new int[] { 0, 5, 10, 12, 15, -1 }) {
            assertThat(DisplayConnector.isBuiltIn(technology), is(false));
        }
    }

    @Test
    void testConnector() {
        DisplayMode mode = readMode(path(1, 1, 60, 1), modes());
        Connector connector = new Connector(10, 2, mode);
        assertThat(connector.getName(), is("DisplayPort-2"));
        assertThat(connector.isBuiltIn(), is(false));
        assertThat(connector.getMode().isPresent(), is(true));
        assertThat(new Connector(11, 0, null).getMode().isPresent(), is(false));
    }
}
