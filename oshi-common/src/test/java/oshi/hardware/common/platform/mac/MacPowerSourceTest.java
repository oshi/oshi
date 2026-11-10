/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

class MacPowerSourceTest {

    @Test
    void testChooseCapacityPrefersRawMah() {
        // Apple Silicon: MaxCapacity is a percentage, AppleRawMaxCapacity is mAh
        assertThat(MacPowerSource.chooseCapacity(7514, 100), is(7514));
        // Older Macs without the raw keys: the unprefixed value is already mAh
        assertThat(MacPowerSource.chooseCapacity(null, 5103), is(5103));
        assertThat(MacPowerSource.chooseCapacity(null, null), is(nullValue()));
    }

    @Test
    void testMinutesToSeconds() {
        assertThat(MacPowerSource.minutesToSeconds(90), is(5400d));
        assertThat(MacPowerSource.minutesToSeconds(0), is(0d));
        assertThat(MacPowerSource.minutesToSeconds(65535), is(-1d));
        assertThat(MacPowerSource.minutesToSeconds(-1), is(-1d));
        assertThat(MacPowerSource.minutesToSeconds(null), is(-1d));
    }
}
