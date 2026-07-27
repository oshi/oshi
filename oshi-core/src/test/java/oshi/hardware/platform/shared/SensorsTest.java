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
