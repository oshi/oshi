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
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.windows.perfmon.ProcessInformationFFM;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read process data from HKEY_PERFORMANCE_DATA information with backup from Performance Counters or WMI
 */
@ThreadSafe
public final class ProcessPerformanceDataFFM {

    private static final String PROCESS = "Process";
    private static final boolean PERFDATA = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_HKEYPERFDATA, true);

    private ProcessPerformanceDataFFM() {
    }

    /**
     * Query the registry for process performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link ProcessPerfCounterBlock} object populated with performance
     *         counter information if successful, or null otherwise.
     */
    public static Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromRegistry(Collection<Integer> pids) {
        // Grab the data from the registry.
        Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData = null;
        if (PERFDATA) {
            processData = HkeyPerformanceDataUtilFFM.readPerfDataFromRegistry(PROCESS,
                    ProcessPerformanceProperty.class);
        }
        if (processData == null) {
            return null;
        }
        List<Map<ProcessPerformanceProperty, Object>> processInstanceMaps = processData.getA();
        long now = processData.getC(); // 1970 epoch

        // Create a map and fill it
        Map<Integer, ProcessPerfCounterBlock> processMap = new HashMap<>();
        // Iterate instances.
        for (Map<ProcessPerformanceProperty, Object> processInstanceMap : processInstanceMaps) {
            int pid = ((Integer) processInstanceMap.get(ProcessPerformanceProperty.IDPROCESS)).intValue();
            String name = (String) processInstanceMap.get(ProcessPerformanceProperty.NAME);
            if ((pids == null || pids.contains(pid)) && !"_Total".equals(name)) {
                // Field name is elapsed time but the value is the process start time
                long ctime = (Long) processInstanceMap.get(ProcessPerformanceProperty.ELAPSEDTIME);
                // if creation time value is less than current millis, it's in 1970 epoch,
                // otherwise it's 1601 epoch and we must convert
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
     * Query PerfMon for process performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link ProcessPerfCounterBlock} object populated with performance
     *         counter information.
     */
    public static Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(Collection<Integer> pids) {
        return buildProcessMapFromPerfCounters(pids, null);
    }

    /**
     * Query PerfMon for process performance counters
     *
     * @param pids     An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @param procName Filter by this process name.
     * @return A map with Process ID as the key and a {@link ProcessPerfCounterBlock} object populated with performance
     *         counter information.
     */
    public static Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(Collection<Integer> pids,
            String procName) {
        Map<Integer, ProcessPerfCounterBlock> processMap = new HashMap<>();
        Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> instanceValues = ProcessInformationFFM
                .queryProcessCounters();
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
                // Field name is elapsed time but the value is the process start time
                long ctime = elapsedTimeList.get(inst);
                // if creation time value is less than current millis, it's in 1970 epoch,
                // otherwise it's 1601 epoch and we must convert
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
