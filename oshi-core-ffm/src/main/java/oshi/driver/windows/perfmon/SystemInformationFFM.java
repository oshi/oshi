/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.SystemInformation;
import oshi.driver.common.windows.perfmon.SystemInformation.ContextSwitchProperty;
import oshi.driver.common.windows.perfmon.SystemInformation.ProcessorQueueLengthProperty;

@ThreadSafe
public final class SystemInformationFFM {
    private SystemInformationFFM() {
    }

    public static Map<ContextSwitchProperty, Long> queryContextSwitchCounters() {
        return SystemInformation.queryContextSwitchCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        return SystemInformation.queryProcessorQueueLength(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
