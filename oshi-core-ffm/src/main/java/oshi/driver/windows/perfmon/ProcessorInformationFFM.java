/*
 * Copyright 2026 The OSHI Project Contributors
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

@ThreadSafe
public final class ProcessorInformationFFM {
    private ProcessorInformationFFM() {
    }

    public static Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters() {
        return ProcessorInformation.queryProcessorCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Map<SystemTickCountProperty, Long> querySystemCounters() {
        return ProcessorInformation.querySystemCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
        return ProcessorInformation.queryProcessorCapacityCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Map<InterruptsProperty, Long> queryInterruptCounters() {
        return ProcessorInformation.queryInterruptCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        return ProcessorInformation.queryFrequencyCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> queryProcessorPerformanceCounters() {
        return ProcessorInformation.queryProcessorPerformanceCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
