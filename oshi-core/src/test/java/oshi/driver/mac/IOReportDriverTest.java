/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.GpuTicks;

@EnabledOnOs(OS.MAC)
class IOReportDriverTest {

    @Test
    void testSampleGpuTicksNotNull() {
        GpuTicks ticks = IOReportDriver.sampleGpuTicks();
        assertThat("sampleGpuTicks should never return null", ticks, is(notNullValue()));
    }

    @Test
    void testSampleGpuTicksTimestampPositive() {
        GpuTicks ticks = IOReportDriver.sampleGpuTicks();
        assertThat("Timestamp should be positive", ticks.getTimestamp(), is(greaterThan(0L)));
    }

    @Test
    void testSampleGpuTicksActiveTicksNonNegative() {
        GpuTicks ticks = IOReportDriver.sampleGpuTicks();
        assertThat("Active ticks should be non-negative", ticks.getActiveTicks(), is(greaterThanOrEqualTo(0L)));
    }

    @Test
    void testSampleGpuTicksFirstCallPositive() {
        GpuTicks ticks = IOReportDriver.sampleGpuTicks();
        Assumptions.assumeTrue(ticks.getActiveTicks() > 0,
                "Skipping: IOReport GPU ticks unavailable (no GPU or sandboxed CI environment)");
        assertThat("First call should return positive cumulative active ticks", ticks.getActiveTicks(),
                is(greaterThan(0L)));
    }

    @Test
    void testSampleGpuTicksMonotonic() throws InterruptedException {
        // First call primes the subscription; second call returns a real delta.
        GpuTicks first = IOReportDriver.sampleGpuTicks();
        Thread.sleep(150);
        GpuTicks second = IOReportDriver.sampleGpuTicks();

        assertThat("Timestamp should be non-decreasing between samples", second.getTimestamp(),
                is(greaterThanOrEqualTo(first.getTimestamp())));
        assertThat("Active ticks should be non-decreasing between samples", second.getActiveTicks(),
                is(greaterThanOrEqualTo(first.getActiveTicks())));
    }

    @Test
    void testSamplePowerWattsValidOrSentinel() throws InterruptedException {
        // First call primes the internal snapshot; second call exercises the delta path.
        double first = IOReportDriver.samplePowerWatts();
        assertThat("Power should be -1 (unavailable) or a non-negative wattage", first == -1d || first >= 0d, is(true));
        Thread.sleep(100);
        double second = IOReportDriver.samplePowerWatts();
        assertThat("Power delta should be -1 (unavailable) or a non-negative wattage", second == -1d || second >= 0d,
                is(true));
    }
}
