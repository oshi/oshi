/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.CentralProcessor.ProcessorCache;
import oshi.hardware.CentralProcessor.TickType;
import oshi.util.Constants;
import oshi.util.tuples.Quartet;

class AixCentralProcessorTest {

    @Test
    void testParseProcessorId() {
        // prtconf excerpt
        Quartet<String, String, String, Boolean> id = AixCentralProcessor.parseProcessorId("""
                System Model: IBM,9114-275
                Processor Type: PowerPC_POWER7
                Processor Version: PV_7_Compat
                Number Of Processors: 8
                CPU Type: 64-bit
                """.lines().toList());
        assertThat(id.getA(), is("IBM"));
        assertThat(id.getB(), is("PowerPC_POWER7"));
        assertThat(id.getC(), is("PV_7_Compat"));
        assertThat(id.getD(), is(true));
    }

    @Test
    void testParseProcessorIdEmpty() {
        Quartet<String, String, String, Boolean> id = AixCentralProcessor.parseProcessorId(Collections.emptyList());
        assertThat(id.getA(), is(Constants.UNKNOWN));
        assertThat(id.getB(), is(""));
        assertThat(id.getC(), is(""));
        assertThat(id.getD(), is(false));
    }

    @Test
    void testParseProcessorIdEmptyMarkerValues() {
        // Bare marker lines (no value after the colon) must not throw; they yield empty fields
        Quartet<String, String, String, Boolean> id = AixCentralProcessor
                .parseProcessorId(List.of("Processor Type:", "Processor Version:", "CPU Type:"));
        assertThat(id.getA(), is(Constants.UNKNOWN));
        assertThat(id.getB(), is(""));
        assertThat(id.getC(), is(""));
        assertThat(id.getD(), is(false));
    }

    @Test
    void testParsePowerVersion() {
        // Real prtconf excerpt from a POWER8 box (IBM,8284-22A): the Processor Version line carries the generation
        List<String> prtconf = List.of(//
                "System Model: IBM,8284-22A", //
                "Processor Type: PowerPC_POWER8", //
                "Processor Implementation Mode: POWER 8", //
                "Processor Version: PV_8_Compat", //
                "Number Of Processors: 10", //
                "CPU Type: 64-bit");
        assertThat(AixCentralProcessor.parsePowerVersion(prtconf), is(8));
        // POWER9 form
        assertThat(AixCentralProcessor.parsePowerVersion(List.of("Processor Version: PV_9_Compat")), is(9));
        // No Processor Version line -> 0 (selects no caches, same as an unknown generation)
        assertThat(AixCentralProcessor.parsePowerVersion(Collections.emptyList()), is(0));
    }

    @Test
    void testParseCurrentFreq() {
        List<String> pmcycles = List.of("Cpu 0 runs at 3000 MHz", "Cpu 1 runs at 3000 MHz");
        // exactly two CPUs
        assertThat(AixCentralProcessor.parseCurrentFreq(pmcycles, 2),
                is(new long[] { 3_000_000_000L, 3_000_000_000L }));
        // more logical processors than output lines: trailing entries stay -1
        assertThat(AixCentralProcessor.parseCurrentFreq(pmcycles, 4),
                is(new long[] { 3_000_000_000L, 3_000_000_000L, -1L, -1L }));
        // no output: all -1
        assertThat(AixCentralProcessor.parseCurrentFreq(Collections.emptyList(), 2), is(new long[] { -1L, -1L }));
    }

    @Test
    void testParseSbits() {
        assertThat(AixCentralProcessor.parseSbits(List.of("#define SBITS 16")), is(16));
        assertThat(AixCentralProcessor.parseSbits(List.of("#define  SBITS  10")), is(10));
        // default when not present
        assertThat(AixCentralProcessor.parseSbits(List.of("#define OTHER 5")), is(16));
        assertThat(AixCentralProcessor.parseSbits(Collections.emptyList()), is(16));
    }

    @Test
    void testCachesForPowerVersion() {
        assertThat(AixCentralProcessor.cachesForPowerVersion(7, 4), hasSize(4));
        assertThat(AixCentralProcessor.cachesForPowerVersion(8, 4), hasSize(5));
        // POWER9 sizes its shared L3 by core count: (cores * 10) MiB
        List<ProcessorCache> p9 = AixCentralProcessor.cachesForPowerVersion(9, 4);
        assertThat(p9, hasSize(4));
        ProcessorCache l3 = p9.stream().filter(c -> c.getLevel() == 3).findFirst().orElseThrow(AssertionError::new);
        assertThat(l3.getCacheSize(), is((4 * 10) << 20));
        // unknown version yields no caches
        assertThat(AixCentralProcessor.cachesForPowerVersion(99, 4), is(empty()));
    }

    @Test
    void testTicksFromRow() {
        AixCentralProcessor.CpuTotalRow row = new AixCentralProcessor.CpuTotalRow();
        row.user = 400;
        row.sys = 200;
        row.idle = 300;
        row.wait = 100;
        // Event counts, not time: must not appear as ticks
        row.devintrs = 1_000_000;
        row.softintrs = 2_000_000;
        // 1000 clock ticks correspond to 1e9 PURR cycles, so 1e8 stolen cycles are 100 clock ticks
        row.puser = 400_000_000L;
        row.psys = 200_000_000L;
        row.pidle = 300_000_000L;
        row.pwait = 100_000_000L;
        row.idle_stolen_purr = 60_000_000L;
        row.busy_stolen_purr = 40_000_000L;

        long[] ticks = AixCentralProcessor.ticksFromRow(row);
        long hz = AixCentralProcessor.USER_HZ;
        assertThat(ticks[TickType.USER.getIndex()], is(400 * 1000L / hz));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(200 * 1000L / hz));
        assertThat(ticks[TickType.IDLE.getIndex()], is(300 * 1000L / hz));
        assertThat(ticks[TickType.IOWAIT.getIndex()], is(100 * 1000L / hz));
        assertThat(ticks[TickType.IRQ.getIndex()], is(0L));
        assertThat(ticks[TickType.SOFTIRQ.getIndex()], is(0L));
        assertThat(ticks[TickType.STEAL.getIndex()], is(100 * 1000L / hz));
    }

    @Test
    void testTicksFromRowNoPurr() {
        AixCentralProcessor.CpuTickRow row = new AixCentralProcessor.CpuTickRow();
        row.user = 100;
        row.busy_stolen_purr = 5_000L;
        assertThat(AixCentralProcessor.ticksFromRow(row)[TickType.STEAL.getIndex()], is(0L));
    }
}
