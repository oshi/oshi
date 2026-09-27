/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.hardware.Display;
import oshi.hardware.DisplayMode;

/**
 * Tests Displays
 */
class DisplayTest {

    /**
     * Test displays
     */
    @Test
    void testDisplay() {
        SystemInfo si = new SystemInfo();
        List<Display> displays = si.getHardware().getDisplays();
        for (Display d : displays) {
            assertThat("EDID Byte length should be at least 128", d.getDisplayInfo().getEdid().length,
                    is(greaterThanOrEqualTo(128)));
            Optional<DisplayMode> current = d.getCurrentMode();
            if (current.isPresent()) {
                DisplayMode mode = current.get();
                assertThat("Width should be positive", mode.getWidth(), is(greaterThan(0)));
                assertThat("Height should be positive", mode.getHeight(), is(greaterThan(0)));
                assertThat("Pixel width should be positive", mode.getPixelWidth(), is(greaterThan(0)));
                assertThat("Pixel height should be positive", mode.getPixelHeight(), is(greaterThan(0)));
                assertThat("Refresh rate should not be negative", mode.getRefreshRate(), is(greaterThanOrEqualTo(0d)));
                assertThat("Rotation should be a quarter turn", mode.getRotation(), is(oneOf(0, 90, 180, 270)));
            }
        }
    }
}
