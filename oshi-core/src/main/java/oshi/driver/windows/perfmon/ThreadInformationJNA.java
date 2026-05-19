/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ThreadInformation;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query Thread performance counter
 */
@ThreadSafe
public final class ThreadInformationJNA {

    private ThreadInformationJNA() {
    }

    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters() {
        return ThreadInformation.queryThreadCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }

    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters(String name,
            int threadNum) {
        return ThreadInformation.queryThreadCounters(PerfCounterQueryExecutorJNA.INSTANCE, name, threadNum);
    }
}
