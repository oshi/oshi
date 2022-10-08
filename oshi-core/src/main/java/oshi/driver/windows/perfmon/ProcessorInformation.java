/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.PROCESSOR;
import static oshi.driver.windows.perfmon.PerfmonConstants.PROCESSOR_INFORMATION;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.VersionHelpers;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query Processor performance counter
 */
@ThreadSafe
public final class ProcessorInformation {

    private static final boolean IS_WIN7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    /**
     * Processor performance counters
     */
    public enum ProcessorTickCountProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PERCENTDPCTIME("% DPC Time"), //
        PERCENTINTERRUPTTIME("% Interrupt Time"), //
        PERCENTPRIVILEGEDTIME("% Privileged Time"), //
        PERCENTPROCESSORTIME("% Processor Time"), //
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
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PERCENTDPCTIME("% DPC Time"), //
        PERCENTINTERRUPTTIME("% Interrupt Time"), //
        PERCENTPRIVILEGEDTIME("% Privileged Time"), //
        PERCENTPROCESSORTIME("% Processor Time"), //
        // The above 3 counters are 100ns base type
        // For PDH accessible as secondary counter in any of them
        TIMESTAMP_SYS100NS("% Processor Time_Base"), //
        PERCENTPRIVILEGEDUTILITY("% Privileged Utility"), //
        PERCENTPROCESSORUTILITY("% Processor Utility"), //
        PERCENTPROCESSORUTILITY_BASE("% Processor Utility_Base"), //
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
        INTERRUPTSPERSEC(PerfCounterQuery.TOTAL_INSTANCE, "Interrupts/sec");

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
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
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

    private ProcessorInformation() {
    }

    /**
     * Returns processor performance counters.
     *
     * @return Performance Counters for processors.
     */
    public static Pair<List<String>, Map<ProcessorTickCountProperty, List<Long>>> queryProcessorCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return IS_WIN7_OR_GREATER ? PerfCounterWildcardQuery.queryInstancesAndValues(ProcessorTickCountProperty.class,
                PROCESSOR_INFORMATION, WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL)
                : PerfCounterWildcardQuery.queryInstancesAndValues(ProcessorTickCountProperty.class, PROCESSOR,
                        WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL);
    }

    /**
     * Returns processor capacity performance counters.
     *
     * @return Performance Counters for processor capacity.
     */
    public static Pair<List<String>, Map<ProcessorUtilityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ProcessorUtilityTickCountProperty.class,
                PROCESSOR_INFORMATION, WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns system interrupts counters.
     *
     * @return Interrupts counter for the total of all processors.
     */
    public static Map<InterruptsProperty, Long> queryInterruptCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(InterruptsProperty.class, PROCESSOR,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
    }

    /**
     * Returns processor frequency counters.
     *
     * @return Processor frequency counter for each processor.
     */
    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ProcessorFrequencyProperty.class, PROCESSOR_INFORMATION,
                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }
}
