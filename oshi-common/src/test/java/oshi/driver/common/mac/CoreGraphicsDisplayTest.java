/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import oshi.hardware.DisplayMode;
import oshi.util.ParseUtil;

class CoreGraphicsDisplayTest {

    // An LG monitor's EDID: manufacturer 0x1e6d (GSM), product code 0x5b08, serial number 0x0b0b0b0b
    private static final byte[] EDID = ParseUtil
            .hexStringToByteArray("00ffffffffffff001e6d085b0b0b0b0b" + "0c1c0104b53c2278fb2eb5ae4f46a527"
                    + "0d5054254b80714f81809500a9c0b300" + "d1c001010101565e00a0a0a029503020"
                    + "35000f282100001a000000fd00384b1e" + "5a1900000a202020202020000000fc00"
                    + "4c4720554c545241474541520000ff00" + "3630344e54505a4832313337370a0100");

    private static final CoreGraphicsDisplay BUILT_IN = new CoreGraphicsDisplay(0x610, 0xa050, 0, true, null);
    private static final CoreGraphicsDisplay LG = new CoreGraphicsDisplay(0x1e6d, 0x5b08, 0x0b0b0b0b, false,
            CoreGraphicsDisplay.toMode(1728, 0, 2560, 1440, 2560, 1440, 60, 0));
    private static final CoreGraphicsDisplay OTHER = new CoreGraphicsDisplay(0x10ac, 0x4114, 0x42543034, false, null);

    @Test
    void testMatchEdidByVendorModelAndSerial() {
        Optional<CoreGraphicsDisplay> match = CoreGraphicsDisplay.matchEdid(List.of(BUILT_IN, LG, OTHER), EDID);
        assertThat(match.isPresent(), is(true));
        assertThat(match.get(), is(sameInstance(LG)));
    }

    @Test
    void testMatchEdidAmbiguousIsEmpty() {
        // Two monitors reporting the same identity cannot be told apart
        CoreGraphicsDisplay twin = new CoreGraphicsDisplay(0x1e6d, 0x5b08, 0x0b0b0b0b, false, null);
        assertThat(CoreGraphicsDisplay.matchEdid(List.of(LG, twin), EDID).isPresent(), is(false));
    }

    @Test
    void testMatchEdidNoMatchOrShortEdid() {
        assertThat(CoreGraphicsDisplay.matchEdid(List.of(BUILT_IN, OTHER), EDID).isPresent(), is(false));
        assertThat(CoreGraphicsDisplay.matchEdid(List.of(LG), new byte[15]).isPresent(), is(false));
        assertThat(CoreGraphicsDisplay.matchEdid(List.of(), EDID).isPresent(), is(false));
    }

    @Test
    void testMatchBuiltIn() {
        assertThat(CoreGraphicsDisplay.matchBuiltIn(List.of(LG, BUILT_IN)).get(), is(sameInstance(BUILT_IN)));
        assertThat(CoreGraphicsDisplay.matchBuiltIn(List.of(LG, OTHER)).isPresent(), is(false));
        assertThat(CoreGraphicsDisplay.matchBuiltIn(List.of(BUILT_IN, BUILT_IN)).isPresent(), is(false));
    }

    @Test
    void testToModeRetina() {
        // A 3456x2234 panel presented as 1728x1117 points, captured from a MacBook Pro
        DisplayMode mode = CoreGraphicsDisplay.toMode(0, 0, 1728, 1117, 3456, 2234, 120, 0);
        assertThat(mode.getWidth(), is(1728));
        assertThat(mode.getHeight(), is(1117));
        assertThat(mode.getPixelWidth(), is(3456));
        assertThat(mode.getPixelHeight(), is(2234));
        assertThat(mode.getRefreshRate(), is(120d));
    }

    @Test
    void testToModeRotatedTurnsPixelSize() {
        // CGDisplayBounds is in the rotated orientation; the mode's pixel size is not
        DisplayMode mode = CoreGraphicsDisplay.toMode(-1080, -300, 1080, 1920, 1920, 1080, 60, 90);
        assertThat(mode.getWidth(), is(1080));
        assertThat(mode.getHeight(), is(1920));
        assertThat(mode.getPixelWidth(), is(1080));
        assertThat(mode.getPixelHeight(), is(1920));
        assertThat(mode.getRotation(), is(90));
        assertThat(mode.getX(), is(-1080));
        assertThat(mode.getY(), is(-300));
    }
}
