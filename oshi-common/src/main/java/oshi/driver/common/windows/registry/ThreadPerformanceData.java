/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import static oshi.driver.common.windows.registry.PerfCounterValues.counterList;
import static oshi.driver.common.windows.registry.PerfCounterValues.intValue;
import static oshi.driver.common.windows.registry.PerfCounterValues.longValue;
import static oshi.driver.common.windows.registry.PerfCounterValues.pointerValue;
import static oshi.driver.common.windows.registry.PerfCounterValues.stringValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Common logic for building thread performance data maps from registry or performance counter results. Callers (JNA/FFM
 * variants) supply the platform-specific pre-fetched registry or performance-counter data.
 */
@ThreadSafe
public final class ThreadPerformanceData {

    /**
     * The performance object name for thread counters.
     */
    public static final String THREAD = "Thread";

    private ThreadPerformanceData() {
    }

    /**
     * Builds a thread map from registry performance data that has already been read.
     *
     * @param pids       An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @param threadData The raw registry data triplet (instance maps, perfTime100nSec, now in ms)
     * @return A map with Thread ID as the key and a {@link ThreadPerfCounterBlock} object populated with performance
     *         counter information, or null if threadData is null.
     */
    public static @Nullable Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(
            @Nullable Collection<Integer> pids,
            @Nullable Triplet<List<Map<ThreadPerformanceProperty, Object>>, Long, Long> threadData) {
        if (threadData == null) {
            return null;
        }
        List<Map<ThreadPerformanceProperty, Object>> threadInstanceMaps = threadData.getA();
        long perfTime100nSec = threadData.getB(); // 1601
        long now = threadData.getC(); // 1970 epoch

        Map<Integer, ThreadPerfCounterBlock> threadMap = new HashMap<>();
        for (Map<ThreadPerformanceProperty, Object> threadInstanceMap : threadInstanceMaps) {
            int pid = intValue(threadInstanceMap, ThreadPerformanceProperty.IDPROCESS);
            int tid = intValue(threadInstanceMap, ThreadPerformanceProperty.IDTHREAD);
            // TID 0 is never a real thread -- thread IDs come from the same pool as PIDs -- so never key the map
            // on it; 0 is the "ID Thread" sentinel the perf-counter path can report for the _Total aggregate or
            // an exiting thread.
            if (tid != 0 && (pids == null || pids.contains(pid)) && pid > 0) {
                String name = stringValue(threadInstanceMap, ThreadPerformanceProperty.NAME);
                long upTime = (perfTime100nSec - longValue(threadInstanceMap, ThreadPerformanceProperty.ELAPSEDTIME))
                        / 10_000L;
                if (upTime < 1) {
                    upTime = 1;
                }
                long user = longValue(threadInstanceMap, ThreadPerformanceProperty.PERCENTUSERTIME) / 10_000L;
                long kernel = longValue(threadInstanceMap, ThreadPerformanceProperty.PERCENTPRIVILEGEDTIME) / 10_000L;
                int priority = intValue(threadInstanceMap, ThreadPerformanceProperty.PRIORITYCURRENT);
                int threadState = intValue(threadInstanceMap, ThreadPerformanceProperty.THREADSTATE);
                int threadWaitReason = intValue(threadInstanceMap, ThreadPerformanceProperty.THREADWAITREASON);
                // Start address is pointer sized when fetched from registry, so this could be
                // either Integer (uint32) or Long depending on OS bitness
                long startAddr = pointerValue(threadInstanceMap, ThreadPerformanceProperty.STARTADDRESS);
                long contextSwitches = Integer
                        .toUnsignedLong(intValue(threadInstanceMap, ThreadPerformanceProperty.CONTEXTSWITCHESPERSEC));
                threadMap.put(tid, new ThreadPerfCounterBlock(name, tid, pid, now - upTime, user, kernel, priority,
                        threadState, threadWaitReason, startAddr, contextSwitches));
            }
        }
        return threadMap;
    }

    /**
     * Builds a thread map from performance counter query results.
     *
     * @param pids           An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @param instanceValues The query results as a pair of (instances, valueMap)
     * @return A map with Thread ID as the key and a {@link ThreadPerfCounterBlock} object populated with performance
     *         counter information, or null if instanceValues is null.
     */
    public static @Nullable Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(
            @Nullable Collection<Integer> pids,
            @Nullable Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> instanceValues) {
        if (instanceValues == null) {
            return null;
        }
        Map<Integer, ThreadPerfCounterBlock> threadMap = new HashMap<>();
        long now = System.currentTimeMillis(); // 1970 epoch
        List<String> instances = instanceValues.getA();
        Map<ThreadPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
        List<Long> tidList = counterList(valueMap, ThreadPerformanceProperty.IDTHREAD);
        List<Long> pidList = counterList(valueMap, ThreadPerformanceProperty.IDPROCESS);
        List<Long> userList = counterList(valueMap, ThreadPerformanceProperty.PERCENTUSERTIME); // 100-nsec
        List<Long> kernelList = counterList(valueMap, ThreadPerformanceProperty.PERCENTPRIVILEGEDTIME); // 100-nsec
        List<Long> startTimeList = counterList(valueMap, ThreadPerformanceProperty.ELAPSEDTIME); // filetime
        List<Long> priorityList = counterList(valueMap, ThreadPerformanceProperty.PRIORITYCURRENT);
        List<Long> stateList = counterList(valueMap, ThreadPerformanceProperty.THREADSTATE);
        List<Long> waitReasonList = counterList(valueMap, ThreadPerformanceProperty.THREADWAITREASON);
        List<Long> startAddrList = counterList(valueMap, ThreadPerformanceProperty.STARTADDRESS);
        List<Long> contextSwitchesList = counterList(valueMap, ThreadPerformanceProperty.CONTEXTSWITCHESPERSEC);

        int nameIndex = 0;
        for (int inst = 0; inst < instances.size(); inst++) {
            int pid = pidList.get(inst).intValue();
            int tid = tidList.get(inst).intValue();
            // TID 0 is never a real thread (thread IDs share the PID pool). PDH reports it as the "ID Thread"
            // sentinel for the _Total aggregate and exiting threads; keying the map on 0 would clobber a real
            // thread with another.
            if (tid != 0 && (pids == null || pids.contains(pid))) {
                String name = Integer.toString(nameIndex++);
                long startTime = startTimeList.get(inst);
                startTime = ParseUtil.filetimeToUtcMs(startTime, false);
                if (startTime > now) {
                    startTime = now - 1;
                }
                long user = userList.get(inst) / 10_000L;
                long kernel = kernelList.get(inst) / 10_000L;
                int priority = priorityList.get(inst).intValue();
                int threadState = stateList.get(inst).intValue();
                int threadWaitReason = waitReasonList.get(inst).intValue();
                long startAddr = startAddrList.get(inst).longValue();
                long contextSwitches = contextSwitchesList.get(inst).longValue();

                threadMap.put(tid, new ThreadPerfCounterBlock(name, tid, pid, startTime, user, kernel, priority,
                        threadState, threadWaitReason, startAddr, contextSwitches));
            }
        }
        return threadMap;
    }
}
