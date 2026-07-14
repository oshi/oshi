/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.TickType;
import oshi.util.tuples.Pair;

/**
 * Tests the native-free tick-mapping and load-average logic hoisted into {@link OpenBsdCentralProcessor}, using a stub
 * that supplies raw CPU-state and load-average values without native calls.
 */
class OpenBsdCentralProcessorTest {

    @Test
    void testFillTicksFiveElement() {
        // Pre-6.4 layout: user, nice, sys, intr, idle
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[] { 10L, 20L, 30L, 40L, 50L },
                new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        assertThat(ticks[TickType.USER.getIndex()], is(10L));
        assertThat(ticks[TickType.NICE.getIndex()], is(20L));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(30L));
        assertThat(ticks[TickType.IRQ.getIndex()], is(40L));
        assertThat(ticks[TickType.IDLE.getIndex()], is(50L));
    }

    @Test
    void testFillTicksSixElement() {
        // OpenBSD 6.4+ inserts a spin element (99) between sys and intr, shifting IRQ and IDLE by one
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[] { 10L, 20L, 30L, 99L, 40L, 50L },
                new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        assertThat(ticks[TickType.USER.getIndex()], is(10L));
        assertThat(ticks[TickType.NICE.getIndex()], is(20L));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(30L));
        assertThat(ticks[TickType.IRQ.getIndex()], is(40L));
        assertThat(ticks[TickType.IDLE.getIndex()], is(50L));
    }

    @Test
    void testFillTicksShortArrayLeavesZeros() {
        // Fewer than CPUSTATES elements: mapping is skipped, ticks stay zero
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[] { 1L, 2L, 3L }, new double[0], 0);
        long[] ticks = cpu.querySystemCpuLoadTicks();
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testLoadAverageRejectsBelowOne() {
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[0], new double[0], 0);
        assertThrows(IllegalArgumentException.class, () -> cpu.getSystemLoadAverage(0));
    }

    @Test
    void testLoadAverageRejectsAboveThree() {
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[0], new double[0], 0);
        assertThrows(IllegalArgumentException.class, () -> cpu.getSystemLoadAverage(4));
    }

    @Test
    void testLoadAveragePreservesFullResult() {
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[0], new double[] { 1.0, 5.0, 15.0 },
                3);
        double[] avg = cpu.getSystemLoadAverage(3);
        assertThat(avg[0], is(1.0));
        assertThat(avg[1], is(5.0));
        assertThat(avg[2], is(15.0));
    }

    @Test
    void testLoadAveragePartialResultFillsNegative() {
        // Native call returns fewer samples than requested: all-or-nothing, whole array becomes -1
        StubOpenBsdCentralProcessor cpu = new StubOpenBsdCentralProcessor(new long[0], new double[] { 1.0, 5.0, 15.0 },
                2);
        double[] avg = cpu.getSystemLoadAverage(3);
        assertThat(avg[0], is(-1.0));
        assertThat(avg[1], is(-1.0));
        assertThat(avg[2], is(-1.0));
    }

    /**
     * Minimal stub supplying raw CPU-state and load-average values without native calls.
     */
    static class StubOpenBsdCentralProcessor extends OpenBsdCentralProcessor {

        private final long[] systemCpTime;
        private final double[] loadavgValues;
        private final int loadavgRetval;

        StubOpenBsdCentralProcessor(long[] systemCpTime, double[] loadavgValues, int loadavgRetval) {
            this.systemCpTime = systemCpTime;
            this.loadavgValues = loadavgValues;
            this.loadavgRetval = loadavgRetval;
        }

        @Override
        protected int sysctl(String name, int def) {
            return def;
        }

        @Override
        protected String sysctl(String name, String def) {
            return def;
        }

        @Override
        protected String sysctl(int[] mib, String def) {
            return def;
        }

        @Override
        protected int sysctl(int[] mib, int def) {
            return def;
        }

        @Override
        protected Pair<Long, Long> queryVmStats() {
            return new Pair<>(0L, 0L);
        }

        @Override
        protected long[] querySystemCpTime() {
            return systemCpTime;
        }

        @Override
        protected long[] queryProcessorCpTime(int cpu) {
            return systemCpTime;
        }

        @Override
        protected int getloadavgNative(double[] loadavg, int nelem) {
            for (int i = 0; i < nelem && i < loadavgValues.length; i++) {
                loadavg[i] = loadavgValues[i];
            }
            return loadavgRetval;
        }
    }
}
