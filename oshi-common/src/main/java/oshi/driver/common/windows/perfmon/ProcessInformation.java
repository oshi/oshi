/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCES;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_INSTANCE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_OR_IDLE_INSTANCES;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;

/**
 * Process performance counter enums
 */
@ThreadSafe
public final class ProcessInformation {

    /**
     * Process performance counters
     */
    public enum ProcessPerformanceProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Process priority base. */
        PRIORITYBASE("Priority Base"),
        /** Process elapsed time. */
        ELAPSEDTIME("Elapsed Time"),
        /** Process ID. */
        IDPROCESS("ID Process"),
        /** Creating process ID. */
        CREATINGPROCESSID("Creating Process ID"),
        /** I/O read bytes per second. */
        IOREADBYTESPERSEC("IO Read Bytes/sec"),
        /** I/O write bytes per second. */
        IOWRITEBYTESPERSEC("IO Write Bytes/sec"),
        /** Working set private bytes. */
        WORKINGSETPRIVATE("Working Set - Private"),
        /** Working set bytes. */
        WORKINGSET("Working Set"),
        /** Page faults per second. */
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
    public enum HandleCountProperty implements PdhCounterProperty {
        /** Handle count for _Total instance. */
        HANDLECOUNT(TOTAL_INSTANCE, "Handle Count");

        private final String instance;
        private final String counter;

        HandleCountProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        @Override
        public String getInstance() {
            return instance;
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
        /** Instance filter (_Total and Idle). */
        NAME(TOTAL_OR_IDLE_INSTANCES),
        /** Percent processor time. */
        PERCENTPROCESSORTIME("% Processor Time"),
        /** Elapsed time. */
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
     * Returns process performance counters.
     *
     * @param executor the performance counter query executor
     * @return Performance Counters for processes, or empty if process counters are disabled.
     */
    public static Pair<List<String>, Map<ProcessPerformanceProperty, List<Long>>> queryProcessCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfProcDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.queryInstancesAndValues(ProcessPerformanceProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns handle count.
     *
     * @param executor the performance counter query executor
     * @return Handle count, or empty map if process counters are disabled.
     */
    public static Map<HandleCountProperty, Long> queryHandles(PerfCounterQueryExecutor executor) {
        if (executor.isPerfProcDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(HandleCountProperty.class, PROCESS, WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL);
    }

    /**
     * Returns idle process counters (for load average calculation).
     *
     * @param executor the performance counter query executor
     * @return Idle process counters, or empty if OS counters are disabled.
     */
    public static Pair<List<String>, Map<IdleProcessorTimeProperty, List<Long>>> queryIdleProcessCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.queryInstancesAndValues(IdleProcessorTimeProperty.class, PROCESS,
                WIN32_PERFPROC_PROCESS_WHERE_IDPROCESS_0);
    }
}
