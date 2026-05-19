/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessorInformation;
import oshi.driver.common.windows.perfmon.ProcessorInformation.InterruptsProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorFrequencyProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorPerformanceProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.ProcessorUtilityTickCountProperty;
import oshi.driver.common.windows.perfmon.ProcessorInformation.SystemTickCountProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query Processor performance counter
 */
@ThreadSafe
public final class ProcessorInformationJNA {

    private ProcessorInformationJNA() {
    }

    public static Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters() {
        return ProcessorInformation.queryProcessorCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Map<SystemTickCountProperty, Long> querySystemCounters() {
        return ProcessorInformation.querySystemCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
        return ProcessorInformation.queryProcessorCapacityCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Map<InterruptsProperty, Long> queryInterruptCounters() {
        return ProcessorInformation.queryInterruptCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        return ProcessorInformation.queryFrequencyCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> queryProcessorPerformanceCounters() {
        return ProcessorInformation.queryProcessorPerformanceCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }
}
