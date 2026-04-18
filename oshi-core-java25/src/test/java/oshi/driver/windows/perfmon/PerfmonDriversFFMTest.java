/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.MEMORY;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PAGING_FILE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PHYSICAL_DISK;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESSOR;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESSOR_INFORMATION;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.SYSTEM;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.THREAD;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_MEMORY;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.driver.common.windows.perfmon.PdhCounterProperty;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.util.platform.windows.PerfCounterQueryFFM;
import oshi.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.util.tuples.Pair;

@EnabledOnOs(OS.WINDOWS)
class PerfmonDriversFFMTest {

    @Test
    void testQueryPageSwaps() {
        Map<PageSwapProperty, Long> pdh = PerfCounterQueryFFM.queryValuesFromPDH(PageSwapProperty.class, MEMORY);
        Map<PageSwapProperty, Long> wmi = PerfCounterQueryFFM.queryValuesFromWMI(PageSwapProperty.class,
                WIN32_PERF_RAW_DATA_PERF_OS_MEMORY);
        assertThat("Failed PDH queryPageSwaps", pdh, is(aMapWithSize(PageSwapProperty.values().length)));
        assertThat("Failed WMI queryPageSwaps", wmi, is(aMapWithSize(PageSwapProperty.values().length)));
        assertNonWildcardValuesClose(pdh, wmi, "PageSwap");
    }

    @Test
    void testQuerySwapUsed() {
        Map<PagingPercentProperty, Long> pdh = PerfCounterQueryFFM.queryValuesFromPDH(PagingPercentProperty.class,
                PAGING_FILE);
        Map<PagingPercentProperty, Long> wmi = PerfCounterQueryFFM.queryValuesFromWMI(PagingPercentProperty.class,
                WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE);
        assertThat("Failed PDH querySwapUsed", pdh, is(aMapWithSize(PagingPercentProperty.values().length)));
        assertThat("Failed WMI querySwapUsed", wmi, is(aMapWithSize(PagingPercentProperty.values().length)));
        assertNonWildcardValuesClose(pdh, wmi, "SwapUsed");
    }

    @Test
    void testQueryDiskCounters() {
        testWildcardCounters("DiskCounters",
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(PhysicalDiskProperty.class, PHYSICAL_DISK),
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(PhysicalDiskProperty.class,
                        WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL));
    }

    @Test
    void testQueryProcessCounters() {
        testWildcardCounters("ProcessCounters",
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(ProcessPerformanceProperty.class, PROCESS),
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(ProcessPerformanceProperty.class,
                        WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL));
    }

    @Test
    void testQueryHandles() {
        Map<HandleCountProperty, Long> pdh = PerfCounterQueryFFM.queryValuesFromPDH(HandleCountProperty.class, PROCESS);
        Map<HandleCountProperty, Long> wmi = PerfCounterQueryFFM.queryValuesFromWMI(HandleCountProperty.class,
                WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL);
        assertThat("Failed PDH queryHandles", pdh, is(aMapWithSize(HandleCountProperty.values().length)));
        assertThat("Failed WMI queryHandles", wmi, is(aMapWithSize(HandleCountProperty.values().length)));
        assertNonWildcardValuesClose(pdh, wmi, "Handles");
    }

    @Test
    void testQueryIdleProcessCounters() {
        testWildcardCounters("IdleProcessCounters",
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(IdleProcessorTimeProperty.class, PROCESS),
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(IdleProcessorTimeProperty.class,
                        WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0));
    }

    @Test
    void testQueryProcessorCounters() {
        testWildcardCounters("ProcessorCounters",
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(ProcessorTickCountProperty.class, PROCESSOR),
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(ProcessorTickCountProperty.class,
                        WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL));
        if (VersionHelpersFFM.IsWindows7OrGreater()) {
            testWildcardCounters("ProcessorCounters(Info)",
                    PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(ProcessorTickCountProperty.class,
                            PROCESSOR_INFORMATION),
                    PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(ProcessorTickCountProperty.class,
                            WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL));

            if (VersionHelpersFFM.IsWindows8OrGreater()) {
                testWildcardCounters("ProcessorUtilityCounters",
                        PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(
                                ProcessorUtilityTickCountProperty.class, PROCESSOR_INFORMATION),
                        PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(
                                ProcessorUtilityTickCountProperty.class,
                                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL));
            }
        }
    }

    @Test
    void testQueryInterruptCounters() {
        Map<InterruptsProperty, Long> pdh = PerfCounterQueryFFM.queryValuesFromPDH(InterruptsProperty.class, PROCESSOR);
        Map<InterruptsProperty, Long> wmi = PerfCounterQueryFFM.queryValuesFromWMI(InterruptsProperty.class,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
        assertThat("Failed PDH queryInterruptCounters", pdh, is(aMapWithSize(InterruptsProperty.values().length)));
        assertThat("Failed WMI queryInterruptCounters", wmi, is(aMapWithSize(InterruptsProperty.values().length)));
        assertNonWildcardValuesClose(pdh, wmi, "Interrupts");
    }

    @Test
    void testQueryFrequencyCounters() {
        testWildcardCounters("FrequencyCounters",
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(ProcessorFrequencyProperty.class,
                        PROCESSOR_INFORMATION),
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(ProcessorFrequencyProperty.class,
                        WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL));
    }

    @Test
    void testQueryContextSwitchCounters() {
        Map<ContextSwitchProperty, Long> pdh = PerfCounterQueryFFM.queryValuesFromPDH(ContextSwitchProperty.class,
                SYSTEM);
        Map<ContextSwitchProperty, Long> wmi = PerfCounterQueryFFM.queryValuesFromWMI(ContextSwitchProperty.class,
                WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
        assertThat("Failed PDH queryContextSwitchCounters", pdh,
                is(aMapWithSize(ContextSwitchProperty.values().length)));
        assertThat("Failed WMI queryContextSwitchCounters", wmi,
                is(aMapWithSize(ContextSwitchProperty.values().length)));
        assertNonWildcardValuesClose(pdh, wmi, "ContextSwitch");
    }

    @Test
    void testQueryThreadCounters() {
        testWildcardCounters("ThreadCounters",
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(ThreadPerformanceProperty.class, THREAD),
                PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(ThreadPerformanceProperty.class,
                        WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL));
    }

    private <T extends Enum<T> & PdhCounterWildcardProperty> void testWildcardCounters(String s,
            Pair<List<String>, Map<T, List<Long>>> pdhData, Pair<List<String>, Map<T, List<Long>>> wmiData) {
        assertWildcardStructure("PDH " + s, pdhData);
        assertWildcardStructure("WMI " + s, wmiData);
    }

    private <T extends Enum<T> & PdhCounterWildcardProperty> void assertWildcardStructure(String s,
            Pair<List<String>, Map<T, List<Long>>> counterData) {
        int instanceCount = counterData.getA().size();
        Map<T, List<Long>> counters = counterData.getB();
        assertThat("Failed " + s + " instances should not be empty", instanceCount > 0, is(true));
        assertThat("Failed " + s + " counters should not be empty", counters.isEmpty(), is(false));
        for (List<Long> counter : counters.values()) {
            assertThat("Failed " + s + " instance count", counter.size(), is(instanceCount));
        }
        int expectedSize = counters.keySet().iterator().next().getDeclaringClass().getEnumConstants().length - 1;
        assertThat("Failed " + s + " map size", counters, is(aMapWithSize(expectedSize)));
    }

    private static <T extends Enum<T> & PdhCounterProperty> void assertNonWildcardValuesClose(Map<T, Long> pdh,
            Map<T, Long> wmi, String label) {
        for (T key : pdh.keySet()) {
            if (wmi.containsKey(key)) {
                long pdhVal = pdh.get(key);
                long wmiVal = wmi.get(key);
                if (pdhVal == 0 && wmiVal == 0) {
                    continue;
                }
                double max = Math.max(Math.abs((double) pdhVal), Math.abs((double) wmiVal));
                double min = Math.min(Math.abs((double) pdhVal), Math.abs((double) wmiVal));
                if (max > 0) {
                    assertThat(label + "." + key.name() + " PDH vs WMI ratio", min / max, is(closeTo(1.0, 0.5)));
                }
            }
        }
    }
}
