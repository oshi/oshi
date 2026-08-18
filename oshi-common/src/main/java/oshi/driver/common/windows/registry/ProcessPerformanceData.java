/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import static oshi.driver.common.windows.registry.PerfCounterValues.counterList;
import static oshi.driver.common.windows.registry.PerfCounterValues.intValue;
import static oshi.driver.common.windows.registry.PerfCounterValues.longValue;
import static oshi.driver.common.windows.registry.PerfCounterValues.stringValue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PerfCounterQueryExecutor;
import oshi.driver.common.windows.perfmon.ProcessInformation;
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
    private static @Nullable Map<Integer, ProcessPerfCounterBlock> mapFromRegistryData(
            @Nullable Collection<Integer> pids,
            @Nullable Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData) {
        if (processData == null) {
            return null;
        }
        List<Map<ProcessPerformanceProperty, Object>> processInstanceMaps = processData.getA();
        long now = processData.getC(); // 1970 epoch

        Map<Integer, ProcessPerfCounterBlock> processMap = new HashMap<>();
        for (Map<ProcessPerformanceProperty, Object> processInstanceMap : processInstanceMaps) {
            int pid = intValue(processInstanceMap, ProcessPerformanceProperty.IDPROCESS);
            String name = stringValue(processInstanceMap, ProcessPerformanceProperty.NAME);
            if ((pids == null || pids.contains(pid)) && !"_Total".equals(name)) {
                long ctime = longValue(processInstanceMap, ProcessPerformanceProperty.ELAPSEDTIME);
                if (ctime > now) {
                    ctime = ParseUtil.filetimeToUtcMs(ctime, false);
                }
                long upTime = now - ctime;
                if (upTime < 1L) {
                    upTime = 1L;
                }
                processMap.put(pid,
                        new ProcessPerfCounterBlock(name,
                                intValue(processInstanceMap, ProcessPerformanceProperty.CREATINGPROCESSID),
                                intValue(processInstanceMap, ProcessPerformanceProperty.PRIORITYBASE),
                                longValue(processInstanceMap, ProcessPerformanceProperty.WORKINGSETPRIVATE),
                                longValue(processInstanceMap, ProcessPerformanceProperty.WORKINGSET), ctime, upTime,
                                longValue(processInstanceMap, ProcessPerformanceProperty.IOREADBYTESPERSEC),
                                longValue(processInstanceMap, ProcessPerformanceProperty.IOWRITEBYTESPERSEC),
                                Integer.toUnsignedLong(
                                        intValue(processInstanceMap, ProcessPerformanceProperty.PAGEFAULTSPERSEC))));
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
    private static @Nullable Map<Integer, ProcessPerfCounterBlock> mapFromCounterValues(
            @Nullable Collection<Integer> pids,
            @Nullable Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> instanceValues) {
        if (instanceValues == null) {
            return null;
        }
        Map<Integer, ProcessPerfCounterBlock> processMap = new HashMap<>();
        long now = System.currentTimeMillis(); // 1970 epoch
        List<String> instances = instanceValues.getA();
        Map<ProcessPerformanceProperty, List<Long>> valueMap = instanceValues.getB();
        List<Long> pidList = counterList(valueMap, ProcessPerformanceProperty.IDPROCESS);
        List<Long> ppidList = counterList(valueMap, ProcessPerformanceProperty.CREATINGPROCESSID);
        List<Long> priorityList = counterList(valueMap, ProcessPerformanceProperty.PRIORITYBASE);
        List<Long> ioReadList = counterList(valueMap, ProcessPerformanceProperty.IOREADBYTESPERSEC);
        List<Long> ioWriteList = counterList(valueMap, ProcessPerformanceProperty.IOWRITEBYTESPERSEC);
        List<Long> privateWorkingSetList = counterList(valueMap, ProcessPerformanceProperty.WORKINGSETPRIVATE);
        List<Long> workingSetList = counterList(valueMap, ProcessPerformanceProperty.WORKINGSET);
        List<Long> elapsedTimeList = counterList(valueMap, ProcessPerformanceProperty.ELAPSEDTIME);
        List<Long> pageFaultsList = counterList(valueMap, ProcessPerformanceProperty.PAGEFAULTSPERSEC);

        for (int inst = 0; inst < instances.size(); inst++) {
            int pid = pidList.get(inst).intValue();
            // Skip PID 0: PDH reports it as the "ID Process" sentinel for the _Total aggregate, the Idle
            // pseudo-process, and any instance whose real PID is not yet available (starting/exiting). Keying
            // the map on 0 would clobber one such entry with another, and no real Windows process has PID 0.
            if (pid != 0 && (pids == null || pids.contains(pid))) {
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

    /**
     * Reads process performance data from the registry.
     *
     * @param executor The backend performing the native queries
     * @param pids     Process IDs to filter to, or null for all
     * @return A map of process ID to counter block, or null if the read failed
     */
    public static @Nullable Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromRegistry(
            PerfCounterQueryExecutor executor, @Nullable Collection<Integer> pids) {
        Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData = null;
        if (HkeyPerformanceDataUtil.PERFDATA) {
            processData = executor.readPerfDataFromRegistry(PROCESS, ProcessPerformanceProperty.class);
        }
        return mapFromRegistryData(pids, processData);
    }

    /**
     * Reads process performance data from performance counters.
     *
     * @param executor The backend performing the native queries
     * @param pids     Process IDs to filter to, or null for all
     * @return A map of process ID to counter block, or null if the read failed
     */
    public static @Nullable Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(
            PerfCounterQueryExecutor executor, @Nullable Collection<Integer> pids) {
        if (executor.isPerfProcDisabled()) {
            return Collections.emptyMap();
        }
        return mapFromCounterValues(pids, ProcessInformation.queryProcessCounters(executor));
    }
}
