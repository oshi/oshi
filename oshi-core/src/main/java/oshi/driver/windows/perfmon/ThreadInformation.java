/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.THREAD;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query Thread Information performance counter
 */
@ThreadSafe
public final class ThreadInformation {

    /**
     * Thread performance counters
     */
    public enum ThreadPerformanceProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PERCENTUSERTIME("% User Time"), //
        PERCENTPRIVILEGEDTIME("% Privileged Time"), //
        ELAPSEDTIME("Elapsed Time"), //
        PRIORITYCURRENT("Priority Current"), //
        STARTADDRESS("Start Address"), //
        THREADSTATE("Thread State"), //
        THREADWAITREASON("Thread Wait Reason"), // 5 is SUSPENDED
        IDPROCESS("ID Process"), //
        IDTHREAD("ID Thread"), //
        CONTEXTSWITCHESPERSEC("Context Switches/sec");

        private final String counter;

        ThreadPerformanceProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private ThreadInformation() {
    }

    /**
     * Returns thread counters.
     *
     * @return Thread counters for each thread.
     */
    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters() {
        if (PerfmonDisabled.PERF_PROC_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
                WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL);
    }
}
