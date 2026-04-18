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
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Common logic for building process performance data maps from registry or performance counter results. Callers
 * (JNA/FFM variants) supply the platform-specific pre-fetched registry or performance-counter data.
 */
@ThreadSafe
public final class ProcessPerformanceData {

    /**
     * The performance object name for process counters.
     */
    public static final String PROCESS = "Process";

    private ProcessPerformanceData() {
    }

    /**
     * Builds a process map from registry performance data that has already been read.
     *
     * @param pids        An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @param processData The raw registry data triplet (instance maps, perfTime100nSec, now in ms)
     * @return A map with Process ID as the key and a {@link ProcessPerfCounterBlock} object populated with performance
     *         counter information, or null if processData is null.
     */
    public static Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromRegistry(Collection<Integer> pids,
            Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData) {
        if (processData == null) {
            return null;
        }
        List<Map<ProcessPerformanceProperty, Object>> processInstanceMaps = processData.getA();
        long now = processData.getC(); // 1970 epoch

        Map<Integer, ProcessPerfCounterBlock> processMap = new HashMap<>();
        for (Map<ProcessPerformanceProperty, Object> processInstanceMap : processInstanceMaps) {
            int pid = ((Integer) processInstanceMap.get(ProcessPerformanceProperty.IDPROCESS)).intValue();
            String name = (String) processInstanceMap.get(ProcessPerformanceProperty.NAME);
            if ((pids == null || pids.contains(pid)) && !"_Total".equals(name)) {
                long ctime = (Long) processInstanceMap.get(ProcessPerformanceProperty.ELAPSEDTIME);
                if (ctime > now) {
                    ctime = ParseUtil.filetimeToUtcMs(ctime, false);
                }
                long upTime = now - ctime;
                if (upTime < 1L) {
                    upTime = 1L;
                }
                processMap.put(pid, new ProcessPerfCounterBlock(name,
                        (Integer) processInstanceMap.get(ProcessPerformanceProperty.CREATINGPROCESSID),
                        (Integer) processInstanceMap.get(ProcessPerformanceProperty.PRIORITYBASE),
                        (Long) processInstanceMap.get(ProcessPerformanceProperty.WORKINGSETPRIVATE),
                        (Long) processInstanceMap.get(ProcessPerformanceProperty.WORKINGSET), ctime, upTime,
                        (Long) processInstanceMap.get(ProcessPerformanceProperty.IOREADBYTESPERSEC),
                        (Long) processInstanceMap.get(ProcessPerformanceProperty.IOWRITEBYTESPERSEC),
                        Integer.toUnsignedLong(
                                (Integer) processInstanceMap.get(ProcessPerformanceProperty.PAGEFAULTSPERSEC))));
            }
        }
        return processMap;
    }

    /**
     * Builds a process map from performance counter query results.
     *
     * @param pids           An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @param instanceValues The query results as a pair of (instances, valueMap)
     * @return A map with Process ID as the key and a {@link ProcessPerfCounterBlock} object populated with performance
     *         counter information, or null if instanceValues is null.
     */
    public static Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(Collection<Integer> pids,
            Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> instanceValues) {
        if (instanceValues == null) {
            return null;
        }
        Map<Integer, ProcessPerfCounterBlock> processMap = new HashMap<>();
        long now = System.currentTimeMillis(); // 1970 epoch
        List<String> instances = instanceValues.getA();
        Map<ProcessPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
        List<Long> pidList = valueMap.get(ProcessPerformanceProperty.IDPROCESS);
        List<Long> ppidList = valueMap.get(ProcessPerformanceProperty.CREATINGPROCESSID);
        List<Long> priorityList = valueMap.get(ProcessPerformanceProperty.PRIORITYBASE);
        List<Long> ioReadList = valueMap.get(ProcessPerformanceProperty.IOREADBYTESPERSEC);
        List<Long> ioWriteList = valueMap.get(ProcessPerformanceProperty.IOWRITEBYTESPERSEC);
        List<Long> privateWorkingSetList = valueMap.get(ProcessPerformanceProperty.WORKINGSETPRIVATE);
        List<Long> workingSetList = valueMap.get(ProcessPerformanceProperty.WORKINGSET);
        List<Long> elapsedTimeList = valueMap.get(ProcessPerformanceProperty.ELAPSEDTIME);
        List<Long> pageFaultsList = valueMap.get(ProcessPerformanceProperty.PAGEFAULTSPERSEC);

        for (int inst = 0; inst < instances.size(); inst++) {
            int pid = pidList.get(inst).intValue();
            if (pids == null || pids.contains(pid)) {
                long ctime = elapsedTimeList.get(inst);
                if (ctime > now) {
                    ctime = ParseUtil.filetimeToUtcMs(ctime, false);
                }
                long upTime = now - ctime;
                if (upTime < 1L) {
                    upTime = 1L;
                }
                processMap.put(pid,
                        new ProcessPerfCounterBlock(instances.get(inst), ppidList.get(inst).intValue(),
                                priorityList.get(inst).intValue(), privateWorkingSetList.get(inst),
                                workingSetList.get(inst), ctime, upTime, ioReadList.get(inst), ioWriteList.get(inst),
                                pageFaultsList.get(inst).longValue()));
            }
        }
        return processMap;
    }
}
