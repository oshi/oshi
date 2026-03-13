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
import java.util.concurrent.TimeUnit;

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
    @SuppressWarnings("resource") // 'captured' is an alias for 'stats'; closed by the try-with-resources above
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

    /**
     * Full lifecycle: sentinel/priming phase, poll phase, shutdown, then verify all methods throw post-close.
     */
    @Test
    void testRealCardSessionFullLifecycle() throws InterruptedException {
        SystemInfo si = new SystemInfo();
        for (GraphicsCard card : si.getHardware().getGraphicsCards()) {
            GpuStats stats = card.createStatsSession();
            try {
                // Phase 1: sentinel / priming — delta-based metrics return -1 on first call
                assertThat("Session open after creation", stats.isClosed(), is(false));
                assertThat("getGpuTicks non-null on first call", stats.getGpuTicks(), is(notNullValue()));
                assertThat("getGpuUtilization >= -1 on first call", stats.getGpuUtilization() >= -1d, is(true));
                assertThat("getPowerDraw >= -1 on first call", stats.getPowerDraw() >= -1d, is(true));
                assertThat("getVramUsed >= -1", stats.getVramUsed() >= -1L, is(true));
                assertThat("getSharedMemoryUsed >= -1", stats.getSharedMemoryUsed() >= -1L, is(true));
                assertThat("getTemperature >= -1", stats.getTemperature() >= -1d, is(true));
                assertThat("getCoreClockMhz >= -1", stats.getCoreClockMhz() >= -1L, is(true));
                assertThat("getMemoryClockMhz >= -1", stats.getMemoryClockMhz() >= -1L, is(true));
                assertThat("getFanSpeedPercent >= -1", stats.getFanSpeedPercent() >= -1d, is(true));

                // Phase 2: poll — after an interval, delta-based metrics return valid value or sentinel
                Thread.sleep(500L);
                assertThat("getGpuTicks non-null after interval", stats.getGpuTicks(), is(notNullValue()));
                assertThat("getGpuUtilization >= -1 after interval", stats.getGpuUtilization() >= -1d, is(true));
                assertThat("getPowerDraw >= -1 after interval", stats.getPowerDraw() >= -1d, is(true));
                assertThat("getVramUsed >= -1 after interval", stats.getVramUsed() >= -1L, is(true));
                assertThat("getTemperature >= -1 after interval", stats.getTemperature() >= -1d, is(true));
            } finally {
                // Phase 3: shutdown
                stats.close();
            }

            // Phase 4: post-close — all metric methods must throw IllegalStateException
            assertThat("Session closed after close()", stats.isClosed(), is(true));
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
    }

    @Test
    void testConcurrentAccessDoesNotThrow() throws Exception {
        GpuStats stats = new NoOpGpuStats();
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                stats.getGpuTicks();
                stats.getTemperature();
                stats.getPowerDraw();
                return null;
            });
        }
        List<Future<Void>> futures;
        try {
            futures = pool.invokeAll(tasks);
        } finally {
            pool.shutdown();
            if (!pool.awaitTermination(5L, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        }
        stats.close();
        for (Future<Void> f : futures) {
            f.get();
        }
    }
}
