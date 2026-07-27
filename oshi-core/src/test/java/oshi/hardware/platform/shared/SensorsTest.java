/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.shared;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notANumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfo;
import oshi.hardware.Sensors;
import oshi.util.platform.mac.SmcUtil;

/**
 * Test Sensors
 */
class SensorsTest {
    private SystemInfo si = new SystemInfo();
    private Sensors s = si.getHardware().getSensors();

    /**
     * Test sensors. Disabled on GitHub Actions due to unreliable LHM sensor readings on Windows runners.
     */
    @Test
    @DisabledIfEnvironmentVariable(named = "GITHUB_ACTIONS", matches = "true")
    void testSensors() {
        assertThat("CPU Temperature should be NaN or between 0 and 100", s.getCpuTemperature(),
                either(notANumber()).or(both(greaterThanOrEqualTo(0d)).and(lessThanOrEqualTo(100d))));
        assertThat("CPU voltage shouldn't be negative", s.getCpuVoltage(), is(greaterThanOrEqualTo(0d)));
    }

    /**
     * Apple Silicon power-gates an idle CPU core cluster, and the SMC then reports a fixed parked value for each die
     * sensor in it - below room ambient, so not a real temperature, but positive. Those must be rejected rather than
     * reported, so a reading is either unavailable or at least the plausibility floor, never in between.
     */
    @Test
    @EnabledOnOs(OS.MAC)
    void testMacObservedSentinelsAreRejected() {
        // Captured from hardware: -4.0 through 5.2 from the iSMC sample reports, 4.633 through 8.425 from a sweep of
        // all 288 temperature sensors on an M2 Max. 8.425 is the highest ever observed and pins the floor.
        for (double sentinel : new double[] { -4.0, 0.0, 2.5, 4.0, 4.633, 5.2, 6.033, 6.7, 7.425, 8.425 }) {
            assertThat("Sentinel " + sentinel + " must be rejected", SmcUtil.isPlausibleTemperature(sentinel),
                    is(false));
        }
        // 21.0 is the lowest genuine reading seen across M1-M5, A18 and Intel T2 machines; no ceiling is applied.
        for (double reading : new double[] { 21.0, 28.0, 35.0, 85.0, 110.0 }) {
            assertThat("Genuine reading " + reading + " must be accepted", SmcUtil.isPlausibleTemperature(reading),
                    is(true));
        }
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void testMacCpuTemperatureIsPlausibleOrUnavailable() {
        assertThat("CPU temperature must be unavailable or plausible, never a parked sensor value",
                s.getCpuTemperature(),
                either(notANumber()).or(is(0d)).or(greaterThanOrEqualTo(SmcUtil.MIN_PLAUSIBLE_TEMPERATURE)));
    }

    @Test
    void testFanSpeeds() {
        int[] speeds = s.getFanSpeeds();
        for (int speed : speeds) {
            assertThat("Fan Speed shouldn't be negative", speed, is(greaterThanOrEqualTo(0)));
        }
    }
}
