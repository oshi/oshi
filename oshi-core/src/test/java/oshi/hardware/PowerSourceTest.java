/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;

/**
 * Test Power Source
 */
class PowerSourceTest {
    /**
     * Test power source.
     */
    @Test
    void testPowerSource() {
        SystemInfo si = new SystemInfo();
        List<PowerSource> psArr = si.getHardware().getPowerSources();
        for (PowerSource ps : psArr) {
            assertThat("Power Source's remaining capacity shouldn't be negative", ps.getRemainingCapacityPercent(),
                    is(greaterThanOrEqualTo(0d)));
            double epsilon = 1E-6;
            assertThat(
                    "Power Source's estimated remaining time should be greater than zero or within error margin of -1 or within error margin of -2",
                    ps.getTimeRemainingEstimated(),
                    is(either(greaterThan(0d)).or(closeTo(-1d, epsilon)).or(closeTo(-2d, epsilon))));
        }
    }
}
