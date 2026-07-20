/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.CentralProcessor.TickType;

class CpuStatTest {

    // /proc/stat overall line: cpu user nice system idle iowait irq softirq steal (guest guest_nice ignored)
    private static final List<String> PROC_STAT = Arrays.asList("cpu  100 200 300 400 500 600 700 800 900 1000",
            "cpu0 10 20 30 40 50 60 70 80 90 100", "cpu1 11 21 31 41 51 61 71 81 91 101", "intr 12345 0 0 0",
            "ctxt 987654", "btime 1600000000", "processes 54321");

    // -------------------------------------------------------------------------
    // Fixture-based parse tests (run on every platform)
    // -------------------------------------------------------------------------

    @Test
    void testParseSystemCpuLoadTicks() {
        long[] ticks = CpuStat.parseSystemCpuLoadTicks(PROC_STAT);
        assertThat(ticks[TickType.USER.getIndex()], is(100L));
        assertThat(ticks[TickType.NICE.getIndex()], is(200L));
        assertThat(ticks[TickType.SYSTEM.getIndex()], is(300L));
        assertThat(ticks[TickType.IDLE.getIndex()], is(400L));
        assertThat(ticks[TickType.IOWAIT.getIndex()], is(500L));
        assertThat(ticks[TickType.IRQ.getIndex()], is(600L));
        assertThat(ticks[TickType.SOFTIRQ.getIndex()], is(700L));
        assertThat(ticks[TickType.STEAL.getIndex()], is(800L));
    }

    @Test
    void testParseSystemCpuLoadTicksEmptyOrShort() {
        // Empty input and a too-short line both yield all-zero ticks
        assertThat(CpuStat.parseSystemCpuLoadTicks(Collections.emptyList())[TickType.USER.getIndex()], is(0L));
        assertThat(CpuStat.parseSystemCpuLoadTicks(Arrays.asList("cpu 1 2"))[TickType.USER.getIndex()], is(0L));
    }

    @Test
    void testParseProcessorCpuLoadTicks() {
        long[][] ticks = CpuStat.parseProcessorCpuLoadTicks(PROC_STAT, 2);
        assertThat(ticks[0][TickType.USER.getIndex()], is(10L));
        assertThat(ticks[0][TickType.IDLE.getIndex()], is(40L));
        assertThat(ticks[1][TickType.USER.getIndex()], is(11L));
        assertThat(ticks[1][TickType.STEAL.getIndex()], is(81L));
    }

    @Test
    void testParseContextSwitches() {
        assertThat(CpuStat.parseContextSwitches(PROC_STAT), is(987654L));
        assertThat(CpuStat.parseContextSwitches(Collections.emptyList()), is(0L));
    }

    @Test
    void testParseInterrupts() {
        assertThat(CpuStat.parseInterrupts(PROC_STAT), is(12345L));
        assertThat(CpuStat.parseInterrupts(Collections.emptyList()), is(0L));
    }

    @Test
    void testParseBootTime() {
        assertThat(CpuStat.parseBootTime(PROC_STAT), is(1600000000L));
        assertThat(CpuStat.parseBootTime(Collections.emptyList()), is(0L));
    }

    // -------------------------------------------------------------------------
    // Smoke tests against the live /proc/stat (Linux only)
    // -------------------------------------------------------------------------

    @Test
    @EnabledOnOs(OS.LINUX)
    void testSystemCpuLoadTicks() {
        long[] systemCpuLoadTicks = CpuStat.getSystemCpuLoadTicks();
        for (long systemCpuTick : systemCpuLoadTicks) {
            assertThat("CPU tick should be greater than or equal to 0", systemCpuTick, greaterThanOrEqualTo(0L));
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testGetProcessorCpuLoadTicks() {
        int logicalProcessorCount = Runtime.getRuntime().availableProcessors();
        long[][] processorCpuLoadTicks = CpuStat.getProcessorCpuLoadTicks(logicalProcessorCount);
        for (long[] cpuTicks : processorCpuLoadTicks) {
            for (long cpuTick : cpuTicks) {
                assertThat("CPU tick should be greater than or equal to 0", cpuTick, greaterThanOrEqualTo(0L));
            }
        }
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testGetContextSwitches() {
        assertThat("Context switches should be greater than or equal to -1", CpuStat.getContextSwitches(),
                greaterThanOrEqualTo(-1L));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testGetInterrupts() {
        assertThat("Interrupts should be greater than or equal to -1", CpuStat.getInterrupts(),
                greaterThanOrEqualTo(-1L));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void testGetBootTime() {
        assertThat("Boot time should be greater than or equal to 0", CpuStat.getBootTime(), greaterThanOrEqualTo(0L));
    }
}
