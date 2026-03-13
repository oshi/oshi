/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import oshi.SystemInfo;
import oshi.hardware.common.NoOpGpuStats;

/**
 * Tests the GpuStats session lifecycle contract.
 */
class GpuStatsTest {

    @Test
    void testNoOpSessionLifecycle() {
        GpuStats stats = new NoOpGpuStats();
        assertThat("Session should be open initially", stats.isClosed(), is(false));
        assertThat("getGpuTicks should return non-null", stats.getGpuTicks(), is(notNullValue()));
        assertThat("getGpuUtilization sentinel", stats.getGpuUtilization(), is(-1d));
        assertThat("getVramUsed sentinel", stats.getVramUsed(), is(-1L));
        assertThat("getSharedMemoryUsed sentinel", stats.getSharedMemoryUsed(), is(-1L));
        assertThat("getTemperature sentinel", stats.getTemperature(), is(-1d));
        assertThat("getPowerDraw sentinel", stats.getPowerDraw(), is(-1d));
        assertThat("getCoreClockMhz sentinel", stats.getCoreClockMhz(), is(-1L));
        assertThat("getMemoryClockMhz sentinel", stats.getMemoryClockMhz(), is(-1L));
        assertThat("getFanSpeedPercent sentinel", stats.getFanSpeedPercent(), is(-1d));
        stats.close();
        assertThat("Session should be closed after close()", stats.isClosed(), is(true));
    }

    @Test
    void testCloseIsIdempotent() {
        GpuStats stats = new NoOpGpuStats();
        stats.close();
        stats.close(); // must not throw
        assertThat(stats.isClosed(), is(true));
    }

    @Test
    void testMetricMethodsThrowAfterClose() {
        GpuStats stats = new NoOpGpuStats();
        stats.close();
        assertThrows(IllegalStateException.class, stats::getGpuTicks);
        assertThrows(IllegalStateException.class, stats::getGpuUtilization);
        assertThrows(IllegalStateException.class, stats::getVramUsed);
        assertThrows(IllegalStateException.class, stats::getSharedMemoryUsed);
        assertThrows(IllegalStateException.class, stats::getTemperature);
        assertThrows(IllegalStateException.class, stats::getPowerDraw);
        assertThrows(IllegalStateException.class, stats::getCoreClockMhz);
        assertThrows(IllegalStateException.class, stats::getMemoryClockMhz);
        assertThrows(IllegalStateException.class, stats::getFanSpeedPercent);
    }

    @Test
    void testTryWithResourcesClosesSession() {
        GpuStats captured;
        try (GpuStats stats = new NoOpGpuStats()) {
            captured = stats;
            assertThat(stats.isClosed(), is(false));
        }
        assertThat("Session must be closed after try-with-resources", captured.isClosed(), is(true));
    }

    @Test
    void testCreateStatsSessionFromRealCard() {
        SystemInfo si = new SystemInfo();
        for (GraphicsCard card : si.getHardware().getGraphicsCards()) {
            try (GpuStats stats = card.createStatsSession()) {
                assertThat("createStatsSession must not return null", stats, is(notNullValue()));
                assertThat("Session must be open", stats.isClosed(), is(false));
                // All metric methods must return sentinel or valid value without throwing
                assertThat(stats.getGpuTicks(), is(notNullValue()));
                assertThat(stats.getGpuUtilization() >= -1d, is(true));
                assertThat(stats.getVramUsed() >= -1L, is(true));
                assertThat(stats.getSharedMemoryUsed() >= -1L, is(true));
                assertThat(stats.getTemperature() >= -1d, is(true));
                assertThat(stats.getPowerDraw() >= -1d, is(true));
                assertThat(stats.getCoreClockMhz() >= -1L, is(true));
                assertThat(stats.getMemoryClockMhz() >= -1L, is(true));
                assertThat(stats.getFanSpeedPercent() >= -1d, is(true));
            }
        }
    }

    @Test
    void testConcurrentAccessDoesNotThrow() throws Exception {
        GpuStats stats = new NoOpGpuStats();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                if (!stats.isClosed()) {
                    stats.getGpuTicks();
                    stats.getTemperature();
                    stats.getPowerDraw();
                }
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        pool.shutdown();
        stats.close();
        for (Future<Void> f : futures) {
            f.get(); // rethrows any exception from the task
        }
    }
}
