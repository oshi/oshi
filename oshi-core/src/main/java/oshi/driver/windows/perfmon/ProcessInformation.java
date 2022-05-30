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

import static oshi.driver.windows.perfmon.PerfmonConstants.PROCESS;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL_OR_IDLE;
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
     * Cooked processor performance counters
     */
    public enum IdleProcessorTimeProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(PerfCounterQuery.TOTAL_OR_IDLE_INSTANCES),
        // Remaining elements define counters
        PERCENTPROCESSORTIME("% Processor Time"), //
        PERCENTPROCESSORTIME_BASE("% Processor Time_Base");

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
                WIN32_PERFPROC_PROCESS_WHERE_NAME_TOTAL_OR_IDLE);
    }
}
