/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.common.windows.registry.ThreadPerformanceData;
import oshi.driver.windows.perfmon.PerfmonDisabledFFM;
import oshi.driver.windows.perfmon.ThreadInformationFFM;
import oshi.util.Util;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read thread data from HKEY_PERFORMANCE_DATA via HkeyPerformanceDataUtilFFM with backup from Performance
 * Counters via ThreadInformationFFM
 */
@ThreadSafe
public final class ThreadPerformanceDataFFM {

    private ThreadPerformanceDataFFM() {
    }

    /**
     * Query the registry for thread performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Thread ID as the key and a {@link ThreadPerfCounterBlock} object populated with performance
     *         counter information if successful, or null otherwise.
     */
    public static Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(Collection<Integer> pids) {
        Triplet<List<Map<ThreadPerformanceProperty, Object>>, Long, Long> threadData = HkeyPerformanceDataUtilFFM
                .readPerfDataFromRegistry(ThreadPerformanceData.THREAD, ThreadPerformanceProperty.class);
        return ThreadPerformanceData.buildThreadMapFromRegistry(pids, threadData);
    }

    /**
     * Query PerfMon for thread performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Thread ID as the key and a {@link ThreadPerfCounterBlock} object populated with performance
     *         counter information.
     */
    public static Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(Collection<Integer> pids) {
        return buildThreadMapFromPerfCounters(pids, null, -1);
    }

    /**
     * Query PerfMon for thread performance counters
     *
     * @param pids      An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @param procName  Limit the matches to processes matching the given name.
     * @param threadNum Limit the matches to threads matching the given thread. Use -1 to match all threads.
     * @return A map with Thread ID as the key and a {@link ThreadPerfCounterBlock} object populated with performance
     *         counter information.
     */
    public static Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(Collection<Integer> pids,
            String procName, int threadNum) {
        if (PerfmonDisabledFFM.PERF_PROC_DISABLED) {
            return Collections.emptyMap();
        }
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> instanceValues = Util.isBlank(procName)
                ? ThreadInformationFFM.queryThreadCounters()
                : ThreadInformationFFM.queryThreadCounters(procName, threadNum);
        return ThreadPerformanceData.buildThreadMapFromPerfCounters(pids, instanceValues);
    }
}
