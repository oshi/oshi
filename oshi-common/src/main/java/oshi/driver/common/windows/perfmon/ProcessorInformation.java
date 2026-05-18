/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCES;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESSOR;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.PROCESSOR_INFORMATION;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_INSTANCE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_FORMATTED_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;

/**
 * Processor performance counter enums
 */
@ThreadSafe
public final class ProcessorInformation {

    /**
     * Processor performance counters
     */
    public enum ProcessorTickCountProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Percent DPC time. */
        PERCENTDPCTIME("% DPC Time"),
        /** Percent interrupt time. */
        PERCENTINTERRUPTTIME("% Interrupt Time"),
        /** Percent privileged time. */
        PERCENTPRIVILEGEDTIME("% Privileged Time"),
        /** Percent processor time. */
        PERCENTPROCESSORTIME("% Processor Time"),
        /** Percent user time. */
        PERCENTUSERTIME("% User Time");

        private final String counter;

        ProcessorTickCountProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * Processor performance counters including utility counters
     */
    public enum ProcessorUtilityTickCountProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Percent DPC time. */
        PERCENTDPCTIME("% DPC Time"),
        /** Percent interrupt time. */
        PERCENTINTERRUPTTIME("% Interrupt Time"),
        /** Percent privileged time. */
        PERCENTPRIVILEGEDTIME("% Privileged Time"),
        /** Percent processor time. */
        PERCENTPROCESSORTIME("% Processor Time"),
        /** Timestamp in 100ns units (SecondValue of % Processor Time). */
        TIMESTAMP_SYS100NS("% Processor Time_Base"),
        /** Percent privileged utility. */
        PERCENTPRIVILEGEDUTILITY("% Privileged Utility"),
        /** Percent processor utility. */
        PERCENTPROCESSORUTILITY("% Processor Utility"),
        /** Percent processor utility base. */
        PERCENTPROCESSORUTILITY_BASE("% Processor Utility_Base"),
        /** Percent user time. */
        PERCENTUSERTIME("% User Time");

        private final String counter;

        ProcessorUtilityTickCountProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * System interrupts counters
     */
    public enum InterruptsProperty implements PdhCounterProperty {
        /** Interrupts per second for _Total instance. */
        INTERRUPTSPERSEC(TOTAL_INSTANCE, "Interrupts/sec");

        private final String instance;
        private final String counter;

        InterruptsProperty(String instance, String counter) {
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
     * Processor Frequency counters. Requires Win7 or greater
     */
    public enum ProcessorFrequencyProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Percent of maximum frequency. */
        PERCENTOFMAXIMUMFREQUENCY("% of Maximum Frequency");

        private final String counter;

        ProcessorFrequencyProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * Processor Performance counters from the WMI Formatted Data table. Reports above 100% with turbo boost. Requires
     * Win8 or greater.
     */
    public enum ProcessorPerformanceProperty implements PdhCounterWildcardProperty {
        /** Instance filter (not _Total). */
        NAME(NOT_TOTAL_INSTANCES),
        /** Percent processor performance (can exceed 100% with turbo). */
        PERCENTPROCESSORPERFORMANCE("% Processor Performance");

        private final String counter;

        ProcessorPerformanceProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * System performance counters
     */
    public enum SystemTickCountProperty implements PdhCounterProperty {
        /** Percent DPC time for _Total. */
        PERCENTDPCTIME(TOTAL_INSTANCE, "% DPC Time"),
        /** Percent interrupt time for _Total. */
        PERCENTINTERRUPTTIME(TOTAL_INSTANCE, "% Interrupt Time");

        private final String instance;
        private final String counter;

        SystemTickCountProperty(String instance, String counter) {
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

    private ProcessorInformation() {
    }

    /**
     * Returns processor performance counters.
     *
     * @param executor the performance counter query executor
     * @return Performance Counters for processors, or empty if OS counters are disabled.
     */
    public static Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.isWin7OrGreater()
                ? executor.queryInstancesAndValues(ProcessorTickCountProperty.class, PROCESSOR_INFORMATION,
                        WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL)
                : executor.queryInstancesAndValues(ProcessorTickCountProperty.class, PROCESSOR,
                        WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL);
    }

    /**
     * Returns system performance counters.
     *
     * @param executor the performance counter query executor
     * @return Performance Counters for the total of all processors, or empty if OS counters are disabled.
     */
    public static Map<SystemTickCountProperty, Long> querySystemCounters(PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(SystemTickCountProperty.class, PROCESSOR,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
    }

    /**
     * Returns processor capacity performance counters.
     *
     * @param executor the performance counter query executor
     * @return Performance Counters for processor capacity, or empty if OS counters are disabled.
     */
    public static Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.queryInstancesAndValues(ProcessorUtilityTickCountProperty.class, PROCESSOR_INFORMATION,
                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns system interrupts counters.
     *
     * @param executor the performance counter query executor
     * @return Interrupts counter for the total of all processors, or empty if OS counters are disabled.
     */
    public static Map<InterruptsProperty, Long> queryInterruptCounters(PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(InterruptsProperty.class, PROCESSOR,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
    }

    /**
     * Returns processor frequency counters.
     *
     * @param executor the performance counter query executor
     * @return Processor frequency counter for each processor, or empty if OS counters are disabled.
     */
    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.queryInstancesAndValues(ProcessorFrequencyProperty.class, PROCESSOR_INFORMATION,
                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns processor performance percentage (cooked) from the WMI Formatted Data table.
     *
     * @param executor the performance counter query executor
     * @return Processor performance percentage for each processor, or empty if OS counters are disabled.
     */
    public static Pair<List<String>, Map<ProcessorPerformanceProperty, List<Long>>> queryProcessorPerformanceCounters(
            PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return executor.queryInstancesAndValuesFromWMI(ProcessorPerformanceProperty.class,
                WIN32_PERF_FORMATTED_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }
}
