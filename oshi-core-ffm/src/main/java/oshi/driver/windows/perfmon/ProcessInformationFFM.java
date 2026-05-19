/*
 * Copyright 2026 The OSHI Project Contributors
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

@ThreadSafe
public final class ProcessInformationFFM {
    private ProcessInformationFFM() {
    }

    public static Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> queryProcessCounters() {
        return ProcessInformation.queryProcessCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Map<HandleCountProperty, Long> queryHandles() {
        return ProcessInformation.queryHandles(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        return ProcessInformation.queryIdleProcessCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
