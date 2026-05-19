/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query Process performance counter
 */
@ThreadSafe
public final class ProcessInformationJNA {

    private ProcessInformationJNA() {
    }

    /**
     * Returns process performance counters.
     *
     * @return Performance Counters for processes.
     */
    public static Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> queryProcessCounters() {
        return ProcessInformation.queryProcessCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    /**
     * Returns handle count.
     *
     * @return Handle count.
     */
    public static Map<HandleCountProperty, Long> queryHandles() {
        return ProcessInformation.queryHandles(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    /**
     * Returns idle process counters.
     *
     * @return Idle process counters.
     */
    public static Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        return ProcessInformation.queryIdleProcessCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }
}
