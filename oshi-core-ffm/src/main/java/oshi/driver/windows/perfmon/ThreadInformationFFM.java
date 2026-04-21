/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.THREAD;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_PROC_THREAD_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.util.tuples.Pair;

/**
 * Utility to query Thread Information performance counter using FFM.
 */
@ThreadSafe
public final class ThreadInformationFFM {

    private ThreadInformationFFM() {
    }

    /**
     * Returns thread counters.
     *
     * @return Thread counters for each thread.
     */
    public static Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> queryThreadCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
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
            Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> threads = PerfCounterWildcardQueryFFM
                    .queryInstancesAndValues(
                            ThreadPerformanceProperty.class, THREAD, WIN32_PERF_RAW_DATA_PERF_PROC_THREAD
                                    + " WHERE Name LIKE \"" + procName + "/_%\" AND IDThread=" + threadNum,
                            procName + "/*");
            if (!threads.getA().isEmpty()) {
                return threads;
            }
        }
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(ThreadPerformanceProperty.class, THREAD,
                WIN32_PERF_RAW_DATA_PERF_PROC_THREAD + " WHERE Name LIKE \"" + procName + "/_%\"", procName + "/*");
    }
}
