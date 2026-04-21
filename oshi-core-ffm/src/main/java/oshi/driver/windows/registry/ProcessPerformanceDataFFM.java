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
import oshi.driver.common.windows.perfmon.ProcessInformation.ProcessPerformanceProperty;
import oshi.driver.common.windows.registry.ProcessPerfCounterBlock;
import oshi.driver.common.windows.registry.ProcessPerformanceData;
import oshi.driver.windows.perfmon.PerfmonDisabledFFM;
import oshi.driver.windows.perfmon.ProcessInformationFFM;
import oshi.util.GlobalConfig;
import oshi.util.tuples.Triplet;

/**
 * Utility to read process data from HKEY_PERFORMANCE_DATA information with backup from Performance Counters or WMI
 */
@ThreadSafe
public final class ProcessPerformanceDataFFM {

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
        Triplet<List<Map<ProcessPerformanceProperty, Object>>, Long, Long> processData = null;
        if (PERFDATA) {
            processData = HkeyPerformanceDataUtilFFM.readPerfDataFromRegistry(ProcessPerformanceData.PROCESS,
                    ProcessPerformanceProperty.class);
        }
        return ProcessPerformanceData.buildProcessMapFromRegistry(pids, processData);
    }

    /**
     * Query PerfMon for process performance counters
     *
     * @param pids An optional collection of process IDs to filter the list to. May be null for no filtering.
     * @return A map with Process ID as the key and a {@link ProcessPerfCounterBlock} object populated with performance
     *         counter information.
     */
    public static Map<Integer, ProcessPerfCounterBlock> buildProcessMapFromPerfCounters(Collection<Integer> pids) {
        if (PerfmonDisabledFFM.PERF_PROC_DISABLED) {
            return Collections.emptyMap();
        }
        return ProcessPerformanceData.buildProcessMapFromPerfCounters(pids,
                ProcessInformationFFM.queryProcessCounters());
    }
}
