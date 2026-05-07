/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESSOR;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESSOR_INFORMATION;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_FORMATTED_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorPerformanceProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.SystemTickCountProperty;
import oshi.ffm.util.platform.windows.PerfCounterQueryFFM;
import oshi.ffm.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.util.tuples.Pair;

/**
 * Utility to query Processor performance counters using FFM.
 */
@ThreadSafe
public final class ProcessorInformationFFM {

    private static final boolean IS_WIN7_OR_GREATER = VersionHelpersFFM.IsWindows7OrGreater();

    private ProcessorInformationFFM() {
    }

    /**
     * Returns processor performance counters.
     *
     * @return Performance Counters for processors.
     */
    public static Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters() {
        return IS_WIN7_OR_GREATER
                ? PerfCounterWildcardQueryFFM.queryInstancesAndValues(ProcessorTickCountProperty.class,
                        PROCESSOR_INFORMATION,
                        WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL)
                : PerfCounterWildcardQueryFFM.queryInstancesAndValues(ProcessorTickCountProperty.class, PROCESSOR,
                        WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL);
    }

    /**
     * Returns system performance counters.
     *
     * @return Performance Counters for the total of all processors.
     */
    public static Map<SystemTickCountProperty, Long> querySystemCounters() {
        return PerfCounterQueryFFM.queryValues(SystemTickCountProperty.class, PROCESSOR,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
    }

    /**
     * Returns processor capacity performance counters.
     *
     * @return Performance Counters for processor capacity.
     */
    public static Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(ProcessorUtilityTickCountProperty.class,
                PROCESSOR_INFORMATION, WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns system interrupts counters.
     *
     * @return Interrupts counter for the total of all processors.
     */
    public static Map<InterruptsProperty, Long> queryInterruptCounters() {
        return PerfCounterQueryFFM.queryValues(InterruptsProperty.class, PROCESSOR,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
    }

    /**
     * Returns processor frequency counters.
     *
     * @return Processor frequency counter for each processor.
     */
    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(ProcessorFrequencyProperty.class,
                PROCESSOR_INFORMATION, WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns processor performance percentage (cooked) from the WMI Formatted Data table.
     *
     * @return Processor performance percentage for each processor, or empty if unavailable.
     */
    public static Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> queryProcessorPerformanceCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromWMI(ProcessorPerformanceProperty.class,
                WIN32_PERF_FORMATTED_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }
}
