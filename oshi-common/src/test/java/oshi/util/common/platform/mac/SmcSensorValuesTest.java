/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import org.junit.jupiter.api.Test;

/**
 * Tests connection-independent SMC sensor value interpretation without Mac hardware.
 * <p>
 * The class and its methods are {@code public} because the module system only opens public types to the JUnit platform;
 * a package-private test class in a named module fails to run.
 */
public class SmcSensorValuesTest { // NOSONAR squid:S5786 - public is required by JPMS, not redundant

    // -- toRpm --

    @Test
    public void testToRpmPreservesZero() {
        // A stopped fan genuinely reads 0.0 (both fans on an idle M3 Pro do). Dropping it would turn stopped fans into
        // no fans; iSMC's < 0.005 filter is deliberately not applied here.
        assertThat(SmcSensorValues.toRpm(0d), is(0));
    }

    @Test
    public void testToRpmClampsNegativeAndNaN() {
        assertThat(SmcSensorValues.toRpm(-5d), is(0));
        assertThat(SmcSensorValues.toRpm(Double.NaN), is(0));
    }

    @Test
    public void testToRpmRoundsRatherThanTruncates() {
        assertThat(SmcSensorValues.toRpm(1349.6d), is(1350));
        assertThat(SmcSensorValues.toRpm(1350.4d), is(1350));
    }

    @Test
    public void testToRpmDoesNotOverflow() {
        assertThat(SmcSensorValues.toRpm(1e30d), is(greaterThanOrEqualTo(0)));
        assertThat(SmcSensorValues.toRpm(1e30d), is(Integer.MAX_VALUE));
    }

    // -- voltage plausibility --

    @Test
    public void testIsPlausibleVoltageRejectsLowAndMisscaled() {
        assertThat(SmcSensorValues.isPlausibleVoltage(0d), is(false));
        assertThat(SmcSensorValues.isPlausibleVoltage(-1d), is(false));
        assertThat("A /1000 mis-scaling of a 1.2 V rail must be rejected", SmcSensorValues.isPlausibleVoltage(0.0012d),
                is(false));
    }

    @Test
    public void testIsPlausibleVoltageAcceptsRealReadings() {
        for (double volts : new double[] { 0.7477d, 0.8638d, 1.2d, 1.35d }) {
            assertThat(volts + " V must be plausible", SmcSensorValues.isPlausibleVoltage(volts), is(true));
        }
    }

    @Test
    public void testNoUpperBoundIsApplied() {
        // No ceiling: since voltage keys are never discovered, no rail key is read, so a ceiling would guard nothing.
        assertThat(SmcSensorValues.isPlausibleVoltage(20.2d), is(true));
    }

    @Test
    public void testFloorSitsBetweenTheTwoPopulations() {
        // The floor separates the mis-scaling target (~0.0012 V) from the lowest confirmed reading (0.7477 V).
        assertThat(SmcSensorValues.MIN_PLAUSIBLE_VOLTAGE, is(greaterThanOrEqualTo(0.0012d)));
        assertThat(SmcSensorValues.MIN_PLAUSIBLE_VOLTAGE, is(lessThanOrEqualTo(0.5d)));
    }

    // -- voltage scaling --

    @Test
    public void testScaleVoltageFpe2IsMillivolts() {
        // fpe2 resolves to 0.25 units; only as millivolts is that a usable voltage resolution.
        assertThat(SmcSensorValues.scaleVoltage(1200d, "fpe2"), is(closeTo(1.2d, 1e-9)));
    }

    @Test
    public void testScaleVoltageOtherTypesAreVolts() {
        assertThat(SmcSensorValues.scaleVoltage(0.7477d, "flt"), is(closeTo(0.7477d, 1e-9)));
        assertThat(SmcSensorValues.scaleVoltage(1.2d, "sp78"), is(closeTo(1.2d, 1e-9)));
        assertThat("An unknown or unread type is treated as volts and left to the floor",
                SmcSensorValues.scaleVoltage(0d, ""), is(closeTo(0d, 1e-9)));
    }
}
