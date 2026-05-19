/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.SystemInformation;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;

/**
 * Utility to query System performance counter
 */
@ThreadSafe
public final class SystemInformationJNA {

    private SystemInformationJNA() {
    }

    public static Map<ContextSwitchProperty, Long> queryContextSwitchCounters() {
        return SystemInformation.queryContextSwitchCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        return SystemInformation.queryProcessorQueueLength(PerfCounterQueryExecutorJNA.INSTANCE);
    }
}
