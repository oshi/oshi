/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.SYSTEM;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;
import oshi.ffm.util.platform.windows.PerfCounterQueryFFM;

/**
 * Utility to query System performance counters using FFM.
 */
@ThreadSafe
public final class SystemInformationFFM {

    private SystemInformationFFM() {
    }

    /**
     * Returns system context switch counters.
     *
     * @return Context switches counter for the total of all processors.
     */
    public static Map<ContextSwitchProperty, Long> queryContextSwitchCounters() {
        return PerfCounterQueryFFM.queryValues(ContextSwitchProperty.class, SYSTEM, WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }

    /**
     * Returns processor queue length.
     *
     * @return Processor Queue Length.
     */
    public static Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        return PerfCounterQueryFFM.queryValues(ProcessorQueueLengthProperty.class, SYSTEM,
                WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }
}
