/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ProcessInformation.HandleCountProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.IdleProcessorTimeProperty;
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.ffm.util.platform.windows.PerfCounterQueryFFM;
import oshi.ffm.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.util.tuples.Pair;

/**
 * Utility to query Process Information performance counter using FFM.
 */
@ThreadSafe
public final class ProcessInformationFFM {

    private ProcessInformationFFM() {
    }

    /**
     * Returns process counters.
     *
     * @return Process counters for each process.
     */
    public static Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> queryProcessCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(ProcessPerformanceProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns handle counters.
     *
     * @return Handle count for the _Total instance.
     */
    public static Map<HandleCountProperty, Long> queryHandles() {
        return PerfCounterQueryFFM.queryValues(HandleCountProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL);
    }

    /**
     * Returns raw idle process performance counters.
     *
     * @return Raw performance counters for idle process.
     */
    public static Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(IdleProcessorTimeProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0);
    }
}
