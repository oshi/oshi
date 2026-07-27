/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notANumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.ffm.util.platform.mac.SmcUtilFFM;
import oshi.spi.SystemInfoFactory;

/**
 * Verifies the FFM Mac sensor plausibility guard against real hardware.
 */
@EnabledOnOs(OS.MAC)
public class MacSensorsPlausibilityFFMTest {

    /**
     * Apple Silicon power-gates an idle CPU core cluster, and the SMC then reports a fixed parked value for each die
     * sensor in it - below room ambient, so not a real temperature, but positive. Those must be rejected rather than
     * reported, so a reading is either unavailable or at least the plausibility floor, never in between.
     */
    @Test
    public void testCpuTemperatureIsPlausibleOrUnavailable() {
        double temp = SystemInfoFactory.create().getHardware().getSensors().getCpuTemperature();
        assertThat("CPU temperature must be unavailable or plausible, never a parked sensor value", temp,
                either(notANumber()).or(is(0d)).or(greaterThanOrEqualTo(SmcUtilFFM.MIN_PLAUSIBLE_TEMPERATURE)));
    }
}
