/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.linux.proc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class CpuStatTest {

    @Test
    void testSystemCpuLoadTicks() {
        long[] systemCpuLoadTicks = CpuStat.getSystemCpuLoadTicks();
        for (long systemCpuTick : systemCpuLoadTicks) {
            assertThat("CPU tick should be greater than or equal to 0", systemCpuTick, greaterThanOrEqualTo(0L));
        }
    }

    @Test
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
    void testGetContextSwitches() {
        assertThat("Context switches should be greater than or equal to -1", CpuStat.getContextSwitches(),
                greaterThanOrEqualTo(-1L));
    }

    @Test
    void testGetInterrupts() {
        assertThat("Interrupts should be greater than or equal to -1", CpuStat.getInterrupts(),
                greaterThanOrEqualTo(-1L));
    }

    @Test
    void testGetBootTime() {
        assertThat("Boot time should be greater than or equal to 0", CpuStat.getBootTime(), greaterThanOrEqualTo(0L));
    }
}
