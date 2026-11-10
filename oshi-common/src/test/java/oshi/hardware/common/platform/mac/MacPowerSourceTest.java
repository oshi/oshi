/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.hardware.PowerSource.CapacityUnits;
import oshi.util.tuples.Triplet;

class MacPowerSourceTest {

    @Test
    void testChooseCapacityKeepsPairInSameUnits() {
        // Apple Silicon: the raw pair is mAh, the unprefixed pair a percentage
        assertCapacity(MacPowerSource.chooseCapacity(5736, 7514, 80, 100), CapacityUnits.MAH, 5736, 7514);
        // Either raw key missing: never mix a raw mAh value with a percentage
        assertCapacity(MacPowerSource.chooseCapacity(null, 7514, 80, 100), CapacityUnits.RELATIVE, 80, 100);
        assertCapacity(MacPowerSource.chooseCapacity(5736, null, 80, 100), CapacityUnits.RELATIVE, 80, 100);
        // Intel Mac without the raw keys: the unprefixed pair is mAh
        assertCapacity(MacPowerSource.chooseCapacity(null, null, 4100, 5103), CapacityUnits.MAH, 4100, 5103);
        // Nothing usable
        assertCapacity(MacPowerSource.chooseCapacity(null, 7514, 80, null), CapacityUnits.RELATIVE, 0, 1);
        // A maximum of zero is not a usable reading in either pair
        assertCapacity(MacPowerSource.chooseCapacity(5736, 0, 80, 100), CapacityUnits.RELATIVE, 80, 100);
        assertCapacity(MacPowerSource.chooseCapacity(null, null, 0, 0), CapacityUnits.RELATIVE, 0, 1);
    }

    private static void assertCapacity(Triplet<CapacityUnits, Integer, Integer> capacity, CapacityUnits units,
            int current, int max) {
        assertThat(capacity.getA(), is(units));
        assertThat(capacity.getB(), is(current));
        assertThat(capacity.getC(), is(max));
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
