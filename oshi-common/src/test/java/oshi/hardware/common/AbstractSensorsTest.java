/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class AbstractSensorsTest {

    private static final AbstractSensors SENSORS = new AbstractSensors() {
        @Override
        protected double queryCpuTemperature() {
            return 55.5;
        }

        @Override
        protected int[] queryFanSpeeds() {
            return new int[] { 1200 };
        }

        @Override
        protected double queryCpuVoltage() {
            return 1.2;
        }
    };

    @Test
    void testMemoizedGetters() {
        assertThat(SENSORS.getCpuTemperature(), is(closeTo(55.5, 0.01)));
        assertThat(SENSORS.getFanSpeeds().length, is(1));
        assertThat(SENSORS.getFanSpeeds()[0], is(1200));
        assertThat(SENSORS.getCpuVoltage(), is(closeTo(1.2, 0.01)));
    }

    @Test
    void testToString() {
        String s = SENSORS.toString();
        assertThat(s, containsString("55.5"));
        assertThat(s, containsString("1200"));
        assertThat(s, containsString("1.2"));
    }
}
