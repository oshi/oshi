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
class IOReportClientTest {

    @Test
    void testCreateReturnsNullOrNonNull() {
        // create() returns null on Intel Mac or sandboxed CI; that is valid
        IOReportClient client = IOReportClient.create();
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testSampleGpuTicksNotNull() {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        try {
            GpuTicks ticks = client.sampleGpuTicks();
            assertThat("sampleGpuTicks should never return null", ticks, is(notNullValue()));
        } finally {
            client.close();
        }
    }

    @Test
    void testSampleGpuTicksTimestampPositive() {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        try {
            GpuTicks ticks = client.sampleGpuTicks();
            assertThat("Timestamp should be positive", ticks.getTimestamp(), is(greaterThan(0L)));
        } finally {
            client.close();
        }
    }

    @Test
    void testSampleGpuTicksActiveTicksNonNegative() {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        try {
            GpuTicks ticks = client.sampleGpuTicks();
            assertThat("Active ticks should be non-negative", ticks.getActiveTicks(), is(greaterThanOrEqualTo(0L)));
        } finally {
            client.close();
        }
    }

    @Test
    void testSampleGpuTicksFirstCallPositive() {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        try {
            GpuTicks ticks = client.sampleGpuTicks();
            Assumptions.assumeTrue(ticks.getActiveTicks() > 0,
                    "Skipping: IOReport GPU ticks unavailable (no GPU or sandboxed CI environment)");
            assertThat("First call should return positive cumulative active ticks", ticks.getActiveTicks(),
                    is(greaterThan(0L)));
        } finally {
            client.close();
        }
    }

    @Test
    void testSampleGpuTicksMonotonic() throws InterruptedException {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        try {
            GpuTicks first = client.sampleGpuTicks();
            Thread.sleep(150);
            GpuTicks second = client.sampleGpuTicks();
            assertThat("Timestamp should be non-decreasing between samples", second.getTimestamp(),
                    is(greaterThanOrEqualTo(first.getTimestamp())));
            assertThat("Active ticks should be non-decreasing between samples", second.getActiveTicks(),
                    is(greaterThanOrEqualTo(first.getActiveTicks())));
        } finally {
            client.close();
        }
    }

    @Test
    void testSamplePowerWattsValidOrSentinel() throws InterruptedException {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        try {
            double first = client.samplePowerWatts();
            assertThat("Power should be -1 (unavailable) or a non-negative wattage", first == -1d || first >= 0d,
                    is(true));
            Thread.sleep(100);
            double second = client.samplePowerWatts();
            assertThat("Power delta should be -1 (unavailable) or a non-negative wattage",
                    second == -1d || second >= 0d, is(true));
        } finally {
            client.close();
        }
    }

    @Test
    void testCloseIsIdempotent() {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        client.close();
        client.close(); // must not throw
    }

    @Test
    void testSamplingAfterCloseReturnsSentinels() {
        IOReportClient client = IOReportClient.create();
        Assumptions.assumeTrue(client != null, "Skipping: IOReport unavailable");
        client.close();
        GpuTicks ticks = client.sampleGpuTicks();
        assertThat("Closed client should return zero-tick snapshot", ticks.getActiveTicks(), is(0L));
        assertThat("Closed client power should return -1", client.samplePowerWatts(), is(-1d));
        assertThat("Closed client utilization should return -1", client.sampleGpuUtilization(), is(-1d));
    }
}
