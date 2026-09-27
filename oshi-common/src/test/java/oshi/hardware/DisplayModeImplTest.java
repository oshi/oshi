/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

class DisplayModeImplTest {

    @Test
    void testGetters() {
        DisplayMode mode = new DisplayModeImpl(-1080, 120, 1080, 1920, 2160, 3840, 59.94, 90);
        assertThat(mode.getX(), is(-1080));
        assertThat(mode.getY(), is(120));
        assertThat(mode.getWidth(), is(1080));
        assertThat(mode.getHeight(), is(1920));
        assertThat(mode.getPixelWidth(), is(2160));
        assertThat(mode.getPixelHeight(), is(3840));
        assertThat(mode.getRefreshRate(), is(59.94));
        assertThat(mode.getRotation(), is(90));
    }

    @Test
    void testRotationNormalizedToQuarterTurns() {
        assertThat(DisplayModeImpl.normalizeRotation(0), is(0));
        assertThat(DisplayModeImpl.normalizeRotation(270), is(270));
        assertThat(DisplayModeImpl.normalizeRotation(360), is(0));
        assertThat(DisplayModeImpl.normalizeRotation(450), is(90));
        assertThat(DisplayModeImpl.normalizeRotation(-90), is(270));
        assertThat(DisplayModeImpl.normalizeRotation(-180), is(180));
        assertThat(DisplayModeImpl.normalizeRotation(89), is(90));
    }

    @Test
    void testNegativeOrNaNRefreshRateIsZero() {
        assertThat(new DisplayModeImpl(0, 0, 1, 1, 1, 1, -1d, 0).getRefreshRate(), is(0d));
        assertThat(new DisplayModeImpl(0, 0, 1, 1, 1, 1, Double.NaN, 0).getRefreshRate(), is(0d));
    }

    @Test
    void testToString() {
        assertThat(new DisplayModeImpl(0, 0, 1920, 1080, 1920, 1080, 60d, 0).toString(),
                is("1920x1080 @ 60.00 Hz at (0,0)"));
        assertThat(new DisplayModeImpl(1920, -200, 1728, 1117, 3456, 2234, 0d, 0).toString(),
                is("1728x1117 (3456x2234 pixels) at (1920,-200)"));
        assertThat(new DisplayModeImpl(0, 0, 1080, 1920, 1080, 1920, 59.93, 270).toString(),
                is("1080x1920 @ 59.93 Hz, rotated 270° at (0,0)"));
    }

    @Test
    void testValueEquality() {
        DisplayMode mode = new DisplayModeImpl(1920, 0, 2560, 1440, 2560, 1440, 59.95, 0);
        DisplayMode same = new DisplayModeImpl(1920, 0, 2560, 1440, 2560, 1440, 59.95, 360);
        assertThat(mode, is(same));
        assertThat(mode.hashCode(), is(same.hashCode()));
        assertThat(mode, is(not(new DisplayModeImpl(1920, 0, 2560, 1440, 2560, 1440, 60d, 0))));
        assertThat(mode, is(not(new DisplayModeImpl(0, 0, 2560, 1440, 2560, 1440, 59.95, 0))));
        assertThat(mode, is(not(new DisplayModeImpl(1920, 0, 2560, 1440, 2560, 1440, 59.95, 90))));
    }
}
