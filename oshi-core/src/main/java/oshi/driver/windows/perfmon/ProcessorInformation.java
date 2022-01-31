/*
 * MIT License
 *
 * Copyright (c) 2020-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import com.sun.jna.platform.win32.VersionHelpers; // NOSONAR squid:s1191

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;
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

    private static final String PROCESSOR = "Processor";
    private static final String PROCESSOR_INFORMATION = "Processor Information";

    // For Win7+ ... NAME field includes NUMA nodes
    private static final String WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE \"%_Total\"";

    // For Vista- ... Older systems just have processor #
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_NOT_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name!=\"_Total\"";
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"";

    private static final boolean IS_WIN7_OR_GREATER = VersionHelpers.IsWindows7OrGreater();

    public static final String OSHI_OS_WINDOWS_CPU_UTILITY = "oshi.os.windows.cpu.utility";
    public static final boolean USE_CPU_UTILITY = VersionHelpers.IsWindows8OrGreater()
            && GlobalConfig.get(OSHI_OS_WINDOWS_CPU_UTILITY, false);
    private static final Pair<List<String>, Map<ProcessorCapacityTickCountProperty, List<Long>>> INITIAL_CAPACITY_TICKS = USE_CPU_UTILITY
            ? queryProcessorCapacityCounters()
            : null;

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
    public enum ProcessorCapacityTickCountProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PERCENTDPCTIME("% DPC Time"), //
        PERCENTINTERRUPTTIME("% Interrupt Time"), //
        PERCENTPRIVILEGEDTIME("% Privileged Time"), //
        PERCENTPROCESSORTIME("% Processor Time"), //
        PERCENTPRIVILEGEDUTILITY("% Privileged Utility"), //
        PERCENTPROCESSORUTILITY("% Processor Utility"), //
        PERCENTPROCESSORUTILITY_BASE("% Processor Utility_Base"), //
        PERCENTUSERTIME("% User Time");

        private final String counter;

        ProcessorCapacityTickCountProperty(String counter) {
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
    public static Pair<List<String>, Map<ProcessorCapacityTickCountProperty, List<Long>>> queryProcessorCapacityCounters() {
        if (!USE_CPU_UTILITY) {
            return null;
        }
        return PerfCounterWildcardQuery.queryInstancesAndValues(ProcessorCapacityTickCountProperty.class,
                PROCESSOR_INFORMATION, WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }

    /**
     * Returns initial processor capacity performance counters.
     *
     * @return initial Performance Counters for processor capacity.
     */
    public static Pair<List<String>, Map<ProcessorCapacityTickCountProperty, List<Long>>> queryInitialProcessorCapacityCounters() {
        return INITIAL_CAPACITY_TICKS;
    }

    /**
     * Returns system interrupts counters.
     *
     * @return Interrupts counter for the total of all processors.
     */
    public static Map<InterruptsProperty, Long> queryInterruptCounters() {
        return PerfCounterQuery.queryValues(InterruptsProperty.class, PROCESSOR,
                WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL);
    }

    /**
     * Returns processor frequency counters.
     *
     * @return Processor frequency counter for each processor.
     */
    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        return PerfCounterWildcardQuery.queryInstancesAndValues(ProcessorFrequencyProperty.class, PROCESSOR_INFORMATION,
                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL);
    }
}
