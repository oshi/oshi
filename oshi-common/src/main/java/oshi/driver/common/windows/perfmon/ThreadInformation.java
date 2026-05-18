/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCES;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.THREAD;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;

/**
 * Thread performance counter enums
 */
@ThreadSafe
public final class ThreadInformation {

    /**
     * Thread performance counters
     */
    public enum ThreadPerformanceProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Percent user time. */
        PERCENTUSERTIME("% User Time"),
        /** Percent privileged time. */
        PERCENTPRIVILEGEDTIME("% Privileged Time"),
        /** Elapsed time. */
        ELAPSEDTIME("Elapsed Time"),
        /** Current priority. */
        PRIORITYCURRENT("Priority Current"),
        /** Start address. */
        STARTADDRESS("Start Address"),
        /** Thread state. */
        THREADSTATE("Thread State"),
        /** Thread wait reason. */
        THREADWAITREASON("Thread Wait Reason"),
        /** Process ID. */
        IDPROCESS("ID Process"),
        /** Thread ID. */
        IDTHREAD("ID Thread"),
        /** Context switches per second. */
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
     * Returns thread counters for all threads.
     *
     * @param executor the performance counter query executor
     * @return Thread counters for each thread.
     */
    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters(
            PerfCounterQueryExecutor executor) {
        return executor.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
                WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns thread counters filtered to the specified process name and thread.
     *
     * @param executor  the performance counter query executor
     * @param name      The process name to filter
     * @param threadNum The thread number to match. -1 matches all threads.
     * @return Thread counters for each thread.
     */
    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters(
            PerfCounterQueryExecutor executor, String name, int threadNum) {
        String procName = name.toLowerCase(Locale.ROOT);
        if (threadNum >= 0) {
            Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> threads = executor
                    .queryInstancesAndValues(
                            ThreadPerformanceProperty.class, THREAD, WIN32_PERF_RAW_DATA_PERF_PROC_THREAD
                                    + " WHERE Name LIKE \"" + procName + "/_%\" AND IDThread=" + threadNum,
                            procName + "/*");
            if (!threads.getA().isEmpty()) {
                return threads;
            }
        }
        return executor.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
                WIN32_PERF_RAW_DATA_PERF_PROC_THREAD + " WHERE Name LIKE \"" + procName + "/_%\"", procName + "/*");
    }
}
