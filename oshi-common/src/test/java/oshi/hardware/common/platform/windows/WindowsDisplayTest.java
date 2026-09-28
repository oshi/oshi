/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.DisplayConnector.Connector;
import oshi.hardware.DisplayMode;
import oshi.hardware.DisplayModeImpl;

class WindowsDisplayTest {

    private static final class TestDisplay extends WindowsDisplay {
        TestDisplay(@Nullable Connector connector) {
            super(new byte[128], connector);
        }
    }

    @Test
    void testUnresolvedConnector() {
        TestDisplay display = new TestDisplay(null);
        assertThat(display.getDevicePort(), is("unknown"));
        assertThat(display.getCurrentMode().isPresent(), is(false));
        assertThat(display.isBuiltIn().isPresent(), is(false));
        assertThat(display.isPrimary(), is(false));
    }

    @Test
    void testResolvedConnector() {
        DisplayMode mode = new DisplayModeImpl(0, 0, 2560, 1600, 2560, 1600, 165d, 0);
        // VOT_DISPLAYPORT_EMBEDDED
        TestDisplay display = new TestDisplay(new Connector(11, 0, mode));
        assertThat(display.getDevicePort(), is("eDP"));
        assertThat(display.getCurrentMode(), is(Optional.of(mode)));
        assertThat(display.isBuiltIn(), is(Optional.of(Boolean.TRUE)));
        // Its desktop origin is (0, 0)
        assertThat(display.isPrimary(), is(true));
    }

    @Test
    void testSecondaryDisplayIsNotPrimary() {
        DisplayMode mode = new DisplayModeImpl(2560, 0, 1920, 1080, 1920, 1080, 60d, 0);
        TestDisplay display = new TestDisplay(new Connector(5, 0, mode));
        assertThat(display.isPrimary(), is(false));
    }

    @Test
    void testResolvedConnectorWithoutMode() {
        // VOT_HDMI
        TestDisplay display = new TestDisplay(new Connector(5, 1, null));
        assertThat(display.getDevicePort(), is("HDMI-1"));
        assertThat(display.getCurrentMode().isPresent(), is(false));
        assertThat(display.isBuiltIn(), is(Optional.of(Boolean.FALSE)));
        assertThat(display.isPrimary(), is(false));
    }
}
