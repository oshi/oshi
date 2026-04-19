/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.windows.registry.ProcessPerformanceDataFFM;
import oshi.driver.windows.registry.ThreadPerformanceDataFFM;
import oshi.util.PlatformEnum;

/**
 * Compares FFM PerfCounters (PDH) and FFM Registry (HKEY_PERFORMANCE_DATA) implementations. Both ultimately read the
 * same kernel performance data, so results should be nearly identical — differences only arise from the time between
 * the two calls and from dynamic fields the scheduler can change.
 */
@DisabledIf("isNotWindows")
class RegistryComparisonTest {

    // Absolute tolerance for the zero-value branch in assertWithinRatio: a process may transiently show 0 bytes
    // for a memory gauge while the other read captures a small allocation.
    private static final long ZERO_TOLERANCE_BYTES = 1024L * 1024L;

    @Test
    void testProcessData() {
        // Registry first, then PerfCounters
        Map<Integer, ProcessPerfCounterBlock> reg = ProcessPerformanceDataFFM.buildProcessMapFromRegistry(null);
        Map<Integer, ProcessPerfCounterBlock> pdh = ProcessPerformanceDataFFM.buildProcessMapFromPerfCounters(null);
        assertThat(reg).as("Registry process map").isNotNull().isNotEmpty();
        assertThat(pdh).as("PerfCounter process map").isNotNull().isNotEmpty();

        // PID sets should have high overlap — processes rarely start/stop in a few ms
        Set<Integer> commonPids = new HashSet<>(reg.keySet());
        commonPids.retainAll(pdh.keySet());
        int minSize = Math.min(reg.size(), pdh.size());
        assertThat(commonPids.size()).as("common PID overlap (reg=%d, pdh=%d)", reg.size(), pdh.size())
                .isGreaterThanOrEqualTo((minSize * 90 + 99) / 100);

        for (int pid : commonPids) {
            ProcessPerfCounterBlock r = reg.get(pid);
            ProcessPerfCounterBlock p = pdh.get(pid);

            // Static fields — must be exact
            assertThat(p.getName()).as("process[%d].name", pid).isEqualTo(r.getName());
            assertThat(p.getParentProcessID()).as("process[%d].ppid", pid).isEqualTo(r.getParentProcessID());
            assertThat(p.getPriority()).as("process[%d].priority", pid).isEqualTo(r.getPriority());
            assertThat(p.getStartTime()).as("process[%d].startTime", pid).isEqualTo(r.getStartTime());

            // Cumulative counters — PDH called second, should be >=
            assertThat(p.getBytesRead()).as("process[%d].bytesRead", pid).isGreaterThanOrEqualTo(r.getBytesRead());
            assertThat(p.getBytesWritten()).as("process[%d].bytesWritten", pid)
                    .isGreaterThanOrEqualTo(r.getBytesWritten());
            assertThat(p.getPageFaults()).as("process[%d].pageFaults", pid).isGreaterThanOrEqualTo(r.getPageFaults());

            // Memory gauges — should be close, allow 50% fluctuation on CI
            assertWithinRatio(p.getWorkingSetSize(), r.getWorkingSetSize(), 0.50,
                    "process[" + pid + "].workingSetSize");
            assertWithinRatio(p.getPrivateWorkingSetSize(), r.getPrivateWorkingSetSize(), 0.50,
                    "process[" + pid + "].privateWorkingSetSize");

            // Uptime differs by the delta between the two reads
            assertThat(Math.abs(p.getUpTime() - r.getUpTime())).as("process[%d].upTime delta", pid)
                    .isLessThanOrEqualTo(Math.max(r.getUpTime() / 10, 1000L));
        }
    }

    @Test
    void testThreadData() {
        // Registry first, then PerfCounters
        Map<Integer, ThreadPerfCounterBlock> reg = ThreadPerformanceDataFFM.buildThreadMapFromRegistry(null);
        Map<Integer, ThreadPerfCounterBlock> pdh = ThreadPerformanceDataFFM.buildThreadMapFromPerfCounters(null);
        assertThat(reg).as("Registry thread map").isNotNull().isNotEmpty();
        assertThat(pdh).as("PerfCounter thread map").isNotNull().isNotEmpty();

        // TID sets should have high overlap
        Set<Integer> commonTids = new HashSet<>(reg.keySet());
        commonTids.retainAll(pdh.keySet());
        int minSize = Math.min(reg.size(), pdh.size());
        assertThat(commonTids.size()).as("common TID overlap (reg=%d, pdh=%d)", reg.size(), pdh.size())
                .isGreaterThanOrEqualTo((minSize * 90 + 99) / 100);

        for (int tid : commonTids) {
            ThreadPerfCounterBlock r = reg.get(tid);
            ThreadPerfCounterBlock p = pdh.get(tid);

            // Static fields — must be exact
            assertThat(p.getOwningProcessID()).as("thread[%d].pid", tid).isEqualTo(r.getOwningProcessID());

            // Priority and thread state are dynamic — scheduler can change them between reads
            assertThat(p.getPriority()).as("thread[%d].priority", tid).isGreaterThanOrEqualTo(0);

            // Cumulative counters — PDH called second, should be >=
            assertThat(p.getUserTime()).as("thread[%d].userTime", tid).isGreaterThanOrEqualTo(r.getUserTime());
            assertThat(p.getKernelTime()).as("thread[%d].kernelTime", tid).isGreaterThanOrEqualTo(r.getKernelTime());
            assertThat(p.getContextSwitches()).as("thread[%d].contextSwitches", tid)
                    .isGreaterThanOrEqualTo(r.getContextSwitches());

            // Start time — allow tolerance for Windows clock tick resolution (1/64s ≈ 15.625ms)
            // and integer division rounding in the now - upTime computation
            assertThat(Math.abs(p.getStartTime() - r.getStartTime())).as("thread[%d].startTime", tid)
                    .isLessThanOrEqualTo(20L);
        }
    }

    // ---- Helpers ----

    private static void assertWithinRatio(double actual, double expected, double ratio, String description) {
        if (expected == 0 && actual == 0) {
            return;
        }
        if (expected == 0 || actual == 0) {
            double nonZero = Math.max(Math.abs(expected), Math.abs(actual));
            assertThat(nonZero).as("%s: one value is 0, other is %.0f", description, nonZero)
                    .isLessThanOrEqualTo(ZERO_TOLERANCE_BYTES);
            return;
        }
        double min = Math.min(Math.abs(actual), Math.abs(expected));
        double max = Math.max(Math.abs(actual), Math.abs(expected));
        assertThat(min / max).as("%s: expected=%f, actual=%f", description, expected, actual)
                .isGreaterThanOrEqualTo(1.0 - ratio);
    }

    static boolean isNotWindows() {
        return PlatformEnum.getCurrentPlatform() != PlatformEnum.WINDOWS;
    }
}
