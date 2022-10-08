/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.SYSTEM;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty;

/**
 * Utility to query System performance counters
 */
@ThreadSafe
public final class SystemInformation {

    /**
     * Context switch property
     */
    public enum ContextSwitchProperty implements PdhCounterProperty {
        CONTEXTSWITCHESPERSEC(null, "Context Switches/sec");

        private final String instance;
        private final String counter;

        ContextSwitchProperty(String instance, String counter) {
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
     * Processor Queue Length property
     */
    public enum ProcessorQueueLengthProperty implements PdhCounterProperty {
        PROCESSORQUEUELENGTH(null, "Processor Queue Length");

        private final String instance;
        private final String counter;

        ProcessorQueueLengthProperty(String instance, String counter) {
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

    private SystemInformation() {
    }

    /**
     * Returns system context switch counters.
     *
     * @return Context switches counter for the total of all processors.
     */
    public static Map<ContextSwitchProperty, Long> queryContextSwitchCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(ContextSwitchProperty.class, SYSTEM, WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }

    /**
     * Returns processor queue length.
     *
     * @return Processor Queue Length.
     */
    public static Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(ProcessorQueueLengthProperty.class, SYSTEM,
                WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }
}
