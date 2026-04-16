/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.tuples.Pair;

/**
 * Utility to query Process Information performance counter
 */
@ThreadSafe
public final class ProcessInformationJNA {

    private ProcessInformationJNA() {
    }

    /**
     * Returns process counters.
     *
     * @return Process counters for each process.
     */
    public static Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> queryProcessCounters() {
        if (PerfmonDisabled.PERF_PROC_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ProcessPerformanceProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns handle counters
     *
     * @return Process handle counters for each process.
     */
    public static Pair<List<String>, Map<HandleCountProperty, List<Long>>> queryHandles() {
        if (PerfmonDisabled.PERF_PROC_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(HandleCountProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS);
    }

    /**
     * Returns cooked idle process performance counters.
     *
     * @return Cooked performance counters for idle process.
     */
    public static Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(IdleProcessorTimeProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0);
    }
}
