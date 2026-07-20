/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.TickType;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

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

    @Test
    void testParseVmStats() {
        // Real OpenBSD vmstat -s: "software interrupts" appears before bare "interrupts"
        List<String> vmstat = Arrays.asList(//
                "      142983 cpu context switches", //
                "           0 software interrupts", //
                "       28301 interrupts", //
                "       50000 some other stat");
        Pair<Long, Long> result = OpenBsdCentralProcessor.parseVmStats(vmstat);
        assertThat(result.getA(), is(142983L));
        assertThat(result.getB(), is(28301L));
    }

    @Test
    void testParseVmStatsEmpty() {
        Pair<Long, Long> result = OpenBsdCentralProcessor.parseVmStats(Collections.emptyList());
        assertThat(result.getA(), is(0L));
        assertThat(result.getB(), is(0L));
    }

    @Test
    void testCpuidToFamilyModelStepping() {
        // Intel Core i7-8700K: CPUID = 0x000906EA → family=6, model=158, stepping=10
        Triplet<Integer, Integer, Integer> fms = OpenBsdCentralProcessor.cpuidToFamilyModelStepping(0x000906EA);
        assertThat(fms.getA(), is(6));
        assertThat(fms.getB(), is(158));
        assertThat(fms.getC(), is(10));
    }

    @Test
    void testCpuidToFamilyModelSteppingAmd() {
        // AMD Zen 2 (Ryzen 3600): CPUID = 0x00800F12 → family=143 (0x8F), model=1, stepping=2
        // formula: family = (cpuid >> 16 & 0xff0) | (cpuid >> 8 & 0xf)
        // model = (cpuid >> 12 & 0xf0) | (cpuid >> 4 & 0xf)
        // 0x00800F12 >> 16 = 0x0080, & 0xff0 = 0x80; >> 8 = 0x00800F, & 0xf = 0xF; family = 0x80|0xF = 0x8F = 143
        // 0x00800F12 >> 12 = 0x00800, & 0xf0 = 0x00; >> 4 = 0x0080_0F1, & 0xf = 0x1; model = 0|1 = 1
        Triplet<Integer, Integer, Integer> fms = OpenBsdCentralProcessor.cpuidToFamilyModelStepping(0x00800F12);
        assertThat(fms.getA(), is(143));
        assertThat(fms.getB(), is(1));
        assertThat(fms.getC(), is(2));
    }

    @Test
    void testCpuidToFamilyModelSteppingZero() {
        Triplet<Integer, Integer, Integer> fms = OpenBsdCentralProcessor.cpuidToFamilyModelStepping(0);
        assertThat(fms.getA(), is(0));
        assertThat(fms.getB(), is(0));
        assertThat(fms.getC(), is(0));
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
