/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.SYSTEM;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * System performance counter enums
 */
@ThreadSafe
public final class SystemInformation {

    /**
     * Context switch property
     */
    public enum ContextSwitchProperty implements PdhCounterProperty {
        /** Context switches per second. */
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
        /** Processor queue length. */
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
     * Returns context switch counters.
     *
     * @param executor the performance counter query executor
     * @return Context switch counters, or empty map if OS counters are disabled.
     */
    public static Map<ContextSwitchProperty, Long> queryContextSwitchCounters(PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(ContextSwitchProperty.class, SYSTEM, WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }

    /**
     * Returns processor queue length.
     *
     * @param executor the performance counter query executor
     * @return Processor Queue Length, or empty map if OS counters are disabled.
     */
    public static Map<ProcessorQueueLengthProperty, Long> queryProcessorQueueLength(PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(ProcessorQueueLengthProperty.class, SYSTEM, WIN32_PERF_RAW_DATA_PERF_OS_SYSTEM);
    }
}
