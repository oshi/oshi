/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.util.tuples.Pair;

class NetBsdCentralProcessorTest {

    @Test
    void testParseVmStats() {
        List<String> vmstat = Arrays.asList(//
                "       42983 CPU context switches", //
                "        8301 device interrupts", //
                "           0 software interrupts", //
                "       12345 some other stat");
        Pair<Long, Long> result = NetBsdCentralProcessor.parseVmStats(vmstat);
        assertThat(result.getA(), is(42983L));
        assertThat(result.getB(), is(8301L));
    }

    @Test
    void testParseVmStatsEmpty() {
        Pair<Long, Long> result = NetBsdCentralProcessor.parseVmStats(Collections.emptyList());
        assertThat(result.getA(), is(0L));
        assertThat(result.getB(), is(0L));
    }

    @Test
    void testParseFamilyModelStepping() {
        List<String> dmesg = Arrays.asList(//
                "cpu0: Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01", //
                "cpu1: Intel(R) Celeron(R) N4000 CPU @ 1.10GHz, 2491.67 MHz, 06-7a-01");
        String[] fms = NetBsdCentralProcessor.parseFamilyModelStepping(dmesg);
        assertThat(fms, is(arrayContaining("06", "7a", "01")));
    }

    @Test
    void testParseFamilyModelSteppingAmd() {
        List<String> dmesg = Arrays.asList(//
                "cpu0: AMD EPYC 7313P 16-Core Processor, 2994.74 MHz, 19-01-01");
        String[] fms = NetBsdCentralProcessor.parseFamilyModelStepping(dmesg);
        assertThat(fms, is(arrayContaining("19", "01", "01")));
    }

    @Test
    void testParseFamilyModelSteppingEmpty() {
        String[] fms = NetBsdCentralProcessor.parseFamilyModelStepping(Collections.emptyList());
        assertThat(fms, is(arrayContaining("", "", "")));
    }

    @Test
    void testParseFamilyModelSteppingNoMatch() {
        List<String> dmesg = Arrays.asList(//
                "cpu0 at mainbus0 mpidr 0: ARM Cortex-A53 r0p4", //
                "cpu0: 32KB 64b/line 2-way L1 VIPT I-cache");
        String[] fms = NetBsdCentralProcessor.parseFamilyModelStepping(dmesg);
        assertThat(fms, is(arrayContaining("", "", "")));
    }

    @Test
    void testParseCpTime() {
        long[] ticks = NetBsdCentralProcessor
                .parseCpTime("kern.cp_time: user = 2930, nice = 42, sys = 1334, intr = 1877, idle = 46354");
        assertThat(ticks[0], is(2930L));
        assertThat(ticks[1], is(42L));
        assertThat(ticks[2], is(1334L));
        assertThat(ticks[3], is(1877L));
        assertThat(ticks[4], is(46354L));
    }

    @Test
    void testParseCpTimeEmpty() {
        long[] ticks = NetBsdCentralProcessor.parseCpTime("");
        assertThat(ticks.length, is(5));
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testParseCpTimeNull() {
        long[] ticks = NetBsdCentralProcessor.parseCpTime(null);
        assertThat(ticks.length, is(5));
        for (long tick : ticks) {
            assertThat(tick, is(0L));
        }
    }

    @Test
    void testParseCpTimePerCpu() {
        long[] ticks = NetBsdCentralProcessor
                .parseCpTime("kern.cp_time.0: user = 100, nice = 0, sys = 50, intr = 10, idle = 840");
        assertThat(ticks[0], is(100L));
        assertThat(ticks[1], is(0L));
        assertThat(ticks[2], is(50L));
        assertThat(ticks[3], is(10L));
        assertThat(ticks[4], is(840L));
    }
}
