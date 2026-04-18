/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public static Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(Collection<Integer> pids,
            Triplet<List<Map<ThreadPerformanceProperty, Object>>, Long, Long> threadData) {
        if (threadData == null) {
            return null;
        }
        List<Map<ThreadPerformanceProperty, Object>> threadInstanceMaps = threadData.getA();
        long perfTime100nSec = threadData.getB(); // 1601
        long now = threadData.getC(); // 1970 epoch

        Map<Integer, ThreadPerfCounterBlock> threadMap = new HashMap<>();
        for (Map<ThreadPerformanceProperty, Object> threadInstanceMap : threadInstanceMaps) {
            Integer pid = (Integer) threadInstanceMap.get(ThreadPerformanceProperty.IDPROCESS);
            if ((pids == null || pids.contains(pid)) && pid > 0) {
                int tid = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.IDTHREAD)).intValue();
                String name = (String) threadInstanceMap.get(ThreadPerformanceProperty.NAME);
                long upTime = (perfTime100nSec - (Long) threadInstanceMap.get(ThreadPerformanceProperty.ELAPSEDTIME))
                        / 10_000L;
                if (upTime < 1) {
                    upTime = 1;
                }
                long user = ((Long) threadInstanceMap.get(ThreadPerformanceProperty.PERCENTUSERTIME)).longValue()
                        / 10_000L;
                long kernel = ((Long) threadInstanceMap.get(ThreadPerformanceProperty.PERCENTPRIVILEGEDTIME))
                        .longValue() / 10_000L;
                int priority = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.PRIORITYCURRENT)).intValue();
                int threadState = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.THREADSTATE)).intValue();
                int threadWaitReason = ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.THREADWAITREASON))
                        .intValue();
                // Start address is pointer sized when fetched from registry, so this could be
                // either Integer (uint32) or Long depending on OS bitness
                Object addr = threadInstanceMap.get(ThreadPerformanceProperty.STARTADDRESS);
                long startAddr = addr.getClass().equals(Long.class) ? (Long) addr
                        : Integer.toUnsignedLong((Integer) addr);
                long contextSwitches = Integer.toUnsignedLong(
                        ((Integer) threadInstanceMap.get(ThreadPerformanceProperty.CONTEXTSWITCHESPERSEC)));
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
    public static Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromPerfCounters(Collection<Integer> pids,
            Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> instanceValues) {
        if (instanceValues == null) {
            return null;
        }
        Map<Integer, ThreadPerfCounterBlock> threadMap = new HashMap<>();
        long now = System.currentTimeMillis(); // 1970 epoch
        List<String> instances = instanceValues.getA();
        Map<ThreadPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
        List<Long> tidList = valueMap.get(ThreadPerformanceProperty.IDTHREAD);
        List<Long> pidList = valueMap.get(ThreadPerformanceProperty.IDPROCESS);
        List<Long> userList = valueMap.get(ThreadPerformanceProperty.PERCENTUSERTIME); // 100-nsec
        List<Long> kernelList = valueMap.get(ThreadPerformanceProperty.PERCENTPRIVILEGEDTIME); // 100-nsec
        List<Long> startTimeList = valueMap.get(ThreadPerformanceProperty.ELAPSEDTIME); // filetime
        List<Long> priorityList = valueMap.get(ThreadPerformanceProperty.PRIORITYCURRENT);
        List<Long> stateList = valueMap.get(ThreadPerformanceProperty.THREADSTATE);
        List<Long> waitReasonList = valueMap.get(ThreadPerformanceProperty.THREADWAITREASON);
        List<Long> startAddrList = valueMap.get(ThreadPerformanceProperty.STARTADDRESS);
        List<Long> contextSwitchesList = valueMap.get(ThreadPerformanceProperty.CONTEXTSWITCHESPERSEC);

        int nameIndex = 0;
        for (int inst = 0; inst < instances.size(); inst++) {
            int pid = pidList.get(inst).intValue();
            if (pids == null || pids.contains(pid)) {
                int tid = tidList.get(inst).intValue();
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
