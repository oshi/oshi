/*
 * Copyright 2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.util.Util;

@EnabledOnOs(OS.WINDOWS)
class LoadAverageTest {

    @Test
    void testQueryLoadAverage() {
        double[] loadAverage = LoadAverage.queryLoadAverage(3);
        assertThat("1m load average should be negative", loadAverage[0], lessThan(0d));
        assertThat("5m load average should be negative", loadAverage[1], lessThan(0d));
        assertThat("15m load average should be negative", loadAverage[2], lessThan(0d));
        LoadAverage.startDaemon();
        Util.sleep(11000L);
        loadAverage = LoadAverage.queryLoadAverage(3);
        assertThat("1m load average should be positive", loadAverage[0], greaterThan(0d));
        assertThat("5m load average should be positive", loadAverage[1], greaterThan(0d));
        assertThat("15m load average should be positive", loadAverage[2], greaterThan(0d));
        LoadAverage.stopDaemon();
    }
}
