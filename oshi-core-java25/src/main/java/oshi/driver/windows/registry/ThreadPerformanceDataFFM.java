/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.ThreadInformation.ThreadPerformanceProperty;
import oshi.driver.common.windows.registry.ThreadPerfCounterBlock;
import oshi.driver.windows.perfmon.ThreadInformationFFM;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read thread data from HKEY_PERFORMANCE_DATA information with backup from Performance Counters or WMI
 */
@ThreadSafe
public final class ThreadPerformanceDataFFM {

    private static final String THREAD = "Thread";

    private ThreadPerformanceDataFFM() {
    }

    /**
     * Query the registry for thread performance counters
     *
     * @param pids An optional collection of thread IDs to filter the list to. May be null for no filtering.
     * @return A map with Thread ID as the key and a {@link ThreadPerfCounterBlock} object populated with performance
     *         counter information if successful, or null otherwise.
     */
    public static Map<Integer, ThreadPerfCounterBlock> buildThreadMapFromRegistry(Collection<Integer> pids) {
        // Grab the data from the registry.
        Triplet<List<Map<ThreadPerformanceProperty, Object>>, Long, Long> threadData = HkeyPerformanceDataUtilFFM
                .readPerfDataFromRegistry(THREAD, ThreadPerformanceProperty.class);
        if (threadData == null) {
            return null;
        }
        List<Map<ThreadPerformanceProperty, Object>> threadInstanceMaps = threadData.getA();
        long perfTime100nSec = threadData.getB(); // 1601
        long now = threadData.getC(); // 1970 epoch

        // Create a map and fill it
        Map<Integer, ThreadPerfCounterBlock> threadMap = new HashMap<>();
        // Iterate instances.
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
        Map<Integer, ThreadPerfCounterBlock> threadMap = new HashMap<>();
        Pair<List<String>, Map<ThreadPerformanceProperty, List<Long>>> instanceValues = Util.isBlank(procName)
                ? ThreadInformationFFM.queryThreadCounters()
                : ThreadInformationFFM.queryThreadCounters(procName, threadNum);
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

                // ParseUtil already converted to UTC ms; clamp future timestamps
                threadMap.put(tid, new ThreadPerfCounterBlock(name, tid, pid, startTime, user, kernel, priority,
                        threadState, threadWaitReason, startAddr, contextSwitches));
            }
        }
        return threadMap;
    }
}
