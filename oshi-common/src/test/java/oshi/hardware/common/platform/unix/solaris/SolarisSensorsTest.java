/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class SolarisSensorsTest {

    @Test
    void testParseCpuTemperature() {
        assertThat(
                SolarisSensors.parseCpuTemperature(
                        Arrays.asList("  :Temperature  42", "    Temperature:  36", "    Temperature:  42")),
                is(closeTo(42.0, 0.001)));
    }

    @Test
    void testParseCpuTemperatureMillidegrees() {
        assertThat(
                SolarisSensors.parseCpuTemperature(Arrays.asList("    Temperature:  45000", "    Temperature:  42000")),
                is(closeTo(45.0, 0.001)));
    }

    @Test
    void testParseCpuTemperatureEmpty() {
        assertThat(SolarisSensors.parseCpuTemperature(Collections.emptyList()), is(closeTo(0.0, 0.001)));
    }

    @Test
    void testParseFanSpeeds() {
        int[] speeds = SolarisSensors
                .parseFanSpeeds(Arrays.asList("    Speed:  1200", "    Speed:  1500", "    Speed:  900"));
        assertThat(speeds.length, is(3));
        assertThat(speeds[0], is(1200));
        assertThat(speeds[1], is(1500));
        assertThat(speeds[2], is(900));
    }

    @Test
    void testParseFanSpeedsEmpty() {
        int[] speeds = SolarisSensors.parseFanSpeeds(Collections.emptyList());
        assertThat(speeds.length, is(0));
    }

    @Test
    void testParseVoltage() {
        assertThat(SolarisSensors.parseVoltage(Arrays.asList("    Voltage:  1.200", "    Voltage:  3.300")),
                is(closeTo(1.2, 0.001)));
    }

    @Test
    void testParseVoltageEmpty() {
        assertThat(SolarisSensors.parseVoltage(Collections.emptyList()), is(closeTo(0.0, 0.001)));
    }

    @Test
    void testParseVoltageNoMatch() {
        assertThat(SolarisSensors.parseVoltage(Arrays.asList("  Other:  1.200", "  Something:  3.300")),
                is(closeTo(0.0, 0.001)));
    }
}
