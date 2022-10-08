/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query Process Information performance counter
 */
@ThreadSafe
public final class ProcessInformation {

    /**
     * Process performance counters
     */
    public enum ProcessPerformanceProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PRIORITYBASE("Priority Base"), //
        ELAPSEDTIME("Elapsed Time"), //
        IDPROCESS("ID Process"), //
        CREATINGPROCESSID("Creating Process ID"), //
        IOREADBYTESPERSEC("IO Read Bytes/sec"), //
        IOWRITEBYTESPERSEC("IO Write Bytes/sec"), //
        PRIVATEBYTES("Working Set - Private"), //
        PAGEFAULTSPERSEC("Page Faults/sec");

        private final String counter;

        ProcessPerformanceProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * Handle performance counters
     */
    public enum HandleCountProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.TOTAL_INSTANCE),
        // Remaining elements define counters
        HANDLECOUNT("Handle Count");

        private final String counter;

        HandleCountProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * Processor performance counters
     */
    public enum IdleProcessorTimeProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.TOTAL_OR_IDLE_INSTANCES),
        // Remaining elements define counters
        PERCENTPROCESSORTIME("% Processor Time"), //
        ELAPSEDTIME("Elapsed Time");

        private final String counter;

        IdleProcessorTimeProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private ProcessInformation() {
    }

    /**
     * Returns process counters.
     *
     * @return Process counters for each process.
     */
    public static Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> queryProcessCounters() {
        if (PerfmonDisabled.PERF_PROC_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ProcessPerformanceProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns handle counters
     *
     * @return Process handle counters for each process.
     */
    public static Pair<List<String>, Map<HandleCountProperty, List<Long>>> queryHandles() {
        if (PerfmonDisabled.PERF_PROC_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(HandleCountProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS);
    }

    /**
     * Returns cooked idle process performance counters.
     *
     * @return Cooked performance counters for idle process.
     */
    public static Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(IdleProcessorTimeProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0);
    }
}
