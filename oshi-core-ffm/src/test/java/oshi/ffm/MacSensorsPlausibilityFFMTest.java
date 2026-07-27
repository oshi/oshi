/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notANumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.util.platform.mac.SmcUtilFFM;
import oshi.spi.SystemInfoFactory;

/**
 * Tests the plausibility guard that keeps an idle-gated SMC sensor's sentinel from being reported as a temperature.
 */
@EnabledOnOs(OS.MAC)
public class MacSensorsPlausibilityFFMTest {

    /**
     * Sentinels actually captured from hardware: -4.0, 0.0, 2.5, 4.0 and 5.2 appear in the iSMC sample reports, and
     * 4.633, 6.033, 6.7, 7.425 and 8.425 in a sweep of all 288 temperature sensors on an M2 Max. 8.425 is the highest
     * ever observed, so it is the value that pins the floor.
     */
    private static final double[] OBSERVED_SENTINELS = { -4.0, 0.0, 2.5, 4.0, 4.633, 5.2, 6.033, 6.7, 7.425, 8.425 };

    /**
     * Genuine readings actually observed: the lowest seen anywhere in the iSMC reports (21.0 on an Intel T2), a typical
     * Apple Silicon idle, and readings under load.
     */
    private static final double[] GENUINE_READINGS = { 21.0, 28.0, 35.0, 52.5, 85.0, 100.0 };

    @Test
    public void testObservedSentinelsAreRejected() {
        for (double sentinel : OBSERVED_SENTINELS) {
            assertThat("Sentinel " + sentinel + " from an idle-gated sensor must be rejected",
                    SmcUtilFFM.isPlausibleTemperature(sentinel), is(false));
        }
    }

    @Test
    public void testGenuineReadingsAreAccepted() {
        for (double reading : GENUINE_READINGS) {
            assertThat("Genuine temperature " + reading + " must be accepted",
                    SmcUtilFFM.isPlausibleTemperature(reading), is(true));
        }
    }

    /**
     * No upper bound is applied. Every bad value observed was at the low end, and a ceiling near the ~100 C throttling
     * point would discard real readings exactly when they matter most. This is a deliberate contract, not an observed
     * reading, so it is asserted separately from {@link #GENUINE_READINGS}.
     */
    @Test
    public void testNoUpperBoundIsApplied() {
        assertThat("A reading above the throttling point must still be accepted",
                SmcUtilFFM.isPlausibleTemperature(110d), is(true));
    }

    /**
     * The guard sits in an empty band: across M1 through M5, A18 and Intel T2 machines, nothing was observed between
     * the highest sentinel and the lowest genuine reading. Failing this means the floor has been moved onto one of the
     * two populations.
     */
    @Test
    public void testFloorSitsBetweenTheTwoObservedPopulations() {
        // Strictly greater: the predicate accepts values equal to the floor, so a floor of exactly 8.425 would
        // accept the highest observed sentinel.
        assertThat("Floor must clear the highest observed sentinel", SmcUtilFFM.MIN_PLAUSIBLE_TEMPERATURE,
                is(greaterThan(8.425)));
        assertThat("Floor must stay below the lowest observed genuine reading", SmcUtilFFM.MIN_PLAUSIBLE_TEMPERATURE,
                is(lessThanOrEqualTo(21.0)));
    }

    /**
     * End to end against the real SMC: a reading is either unavailable or plausible, never a sentinel in between.
     */
    @Test
    public void testCpuTemperatureIsPlausibleOrUnavailable() {
        double temp = SystemInfoFactory.create().getHardware().getSensors().getCpuTemperature();
        assertThat("CPU temperature must be unavailable or plausible, never a sentinel", temp,
                either(notANumber()).or(is(0d)).or(greaterThanOrEqualTo(SmcUtilFFM.MIN_PLAUSIBLE_TEMPERATURE)));
    }
}
