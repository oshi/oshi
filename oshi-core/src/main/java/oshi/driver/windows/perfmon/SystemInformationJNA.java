/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.SYSTEM;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.util.platform.windows.PerfCounterQuery;

/**
 * Utility to query System performance counters
 */
@ThreadSafe
public final class SystemInformationJNA {

    private SystemInformationJNA() {
    }

    /**
     * Returns system context switch counters.
     *
     * @return Context switches counter for the total of all processors.
     */
    public static Map<ContextSwitchProperty, Long> queryContextSwitchCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(ContextSwitchProperty.class, SYSTEM, WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }

    /**
     * Returns processor queue length.
     *
     * @return Processor Queue Length.
     */
    public static Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(ProcessorQueueLengthProperty.class, SYSTEM,
                WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }
}
