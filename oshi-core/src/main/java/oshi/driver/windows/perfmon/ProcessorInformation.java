/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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

import javax.annotation.concurrent.ThreadSafe;

import com.sun.jna.platform.win32.VersionHelpers; // NOSONAR squid:s1191

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
    private static final String INTERRUPT_COUNT = "Interrupt Count";
    private static final String PROCESSOR_FREQUENCY = "Processor Frequency";
    private static final String PROCESSOR_INFORMATION = "Processor Information";
    private static final String PROCESSOR_TICK_COUNT = "Processor Tick Count";

    // For Win7+ ... NAME field includes NUMA nodes
    private static final String WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL = "Win32_PerfRawData_Counters_ProcessorInformation WHERE NOT Name LIKE\"%_Total\"";

    // For Vista- ... Older systems just have processor #
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NOT_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE NOT Name=\"_Total\"";
    private static final String WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL = "Win32_PerfRawData_PerfOS_Processor WHERE Name=\"_Total\"";

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
     * System performance counters
     */
    public enum SystemTickCountProperty implements PdhCounterProperty {
        PERCENTDPCTIME(PerfCounterQuery.TOTAL_INSTANCE, "% DPC Time"), //
        PERCENTINTERRUPTTIME(PerfCounterQuery.TOTAL_INSTANCE, "% Interrupt Time");

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
        PerfCounterWildcardQuery<ProcessorTickCountProperty> processorTickPerfCounters = IS_WIN7_OR_GREATER
                ? new PerfCounterWildcardQuery<>(ProcessorTickCountProperty.class, PROCESSOR_INFORMATION,
                        WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL,
                        PROCESSOR_TICK_COUNT)
                : new PerfCounterWildcardQuery<>(ProcessorTickCountProperty.class, PROCESSOR,
                        WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NOT_NAME_TOTAL, PROCESSOR_TICK_COUNT);
        Map<ProcessorTickCountProperty, List<Long>> values = processorTickPerfCounters.queryValuesWildcard();
        List<String> instances = processorTickPerfCounters.getInstancesFromLastQuery();
        return new Pair<>(instances, values);
    }

    /**
     * Returns system performance counters.
     *
     * @return Performance Counters for the total of all processors.
     */
    public static Map<SystemTickCountProperty, Long> querySystemCounters() {
        PerfCounterQuery<SystemTickCountProperty> systemTickPerfCounters = new PerfCounterQuery<>(
                SystemTickCountProperty.class, PROCESSOR, WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL,
                "System Tick Count");
        return systemTickPerfCounters.queryValues();
    }

    /**
     * Returns system interrupts counters.
     *
     * @return Interrupts counter for the total of all processors.
     */
    public static Map<InterruptsProperty, Long> queryInterruptCounters() {
        PerfCounterQuery<InterruptsProperty> interruptsPerfCounters = new PerfCounterQuery<>(InterruptsProperty.class,
                PROCESSOR, WIN32_PERF_RAW_DATA_PERF_OS_PROCESSOR_WHERE_NAME_TOTAL, INTERRUPT_COUNT);
        return interruptsPerfCounters.queryValues();
    }

    /**
     * Returns processor frequency counters.
     *
     * @return Processor frequency counter for each processor.
     */
    public static Pair<List<String>, Map<ProcessorFrequencyProperty, List<Long>>> queryFrequencyCounters() {
        PerfCounterWildcardQuery<ProcessorFrequencyProperty> processorFrequencyCounters = new PerfCounterWildcardQuery<>(
                ProcessorFrequencyProperty.class, PROCESSOR_INFORMATION,
                WIN32_PERF_RAW_DATA_COUNTERS_PROCESSOR_INFORMATION_WHERE_NOT_NAME_LIKE_TOTAL, PROCESSOR_FREQUENCY);
        Map<ProcessorFrequencyProperty, List<Long>> values = processorFrequencyCounters.queryValuesWildcard();
        List<String> instances = processorFrequencyCounters.getInstancesFromLastQuery();
        return new Pair<>(instances, values);
    }
}
