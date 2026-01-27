/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notANumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import oshi.SystemInfo;

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

    @Test
    void testFanSpeeds() {
        int[] speeds = s.getFanSpeeds();
        for (int speed : speeds) {
            assertThat("Fan Speed shouldn't be negative", speed, is(greaterThanOrEqualTo(0)));
        }
    }
}
