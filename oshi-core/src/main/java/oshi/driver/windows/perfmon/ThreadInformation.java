/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.THREAD;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.List;
import java.util.Locale;
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
        return PerfCounterWildcardQuery.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
                WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns thread counters filtered to the specified process name and thread.
     *
     * @param name      The process name to filter
     * @param threadNum The thread number to match. -1 matches all threads.
     *
     * @return Thread counters for each thread.
     */
    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters(String name,
            int threadNum) {
        String procName = name.toLowerCase(Locale.ROOT);
        if (threadNum >= 0) {
            return PerfCounterWildcardQuery.queryInstancesAndValues(
                    ThreadPerformanceProperty.class, THREAD, WIN32_PERF_RAW_DATA_PERF_PROC_THREAD
                            + " WHERE Name LIKE \\\"" + procName + "\\\" AND IDThread=" + threadNum,
                    procName + "/" + threadNum);
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
                WIN32_PERF_RAW_DATA_PERF_PROC_THREAD + " WHERE Name LIKE \\\"" + procName + "\\\"", procName + "/*");
    }
}
