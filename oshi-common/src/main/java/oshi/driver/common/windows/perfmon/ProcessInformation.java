/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCES;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_INSTANCE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_OR_IDLE_INSTANCES;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Process performance counter enums
 */
@ThreadSafe
public final class ProcessInformation {

    /**
     * Process performance counters
     */
    public enum ProcessPerformanceProperty implements PdhCounterWildcardProperty {
        // First element defines WMI instance name field and PDH instance filter
        NAME(NOT_TOTAL_INSTANCES),
        // Remaining elements define counters
        PRIORITYBASE("Priority Base"), //
        ELAPSEDTIME("Elapsed Time"), //
        IDPROCESS("ID Process"), //
        CREATINGPROCESSID("Creating Process ID"), //
        IOREADBYTESPERSEC("IO Read Bytes/sec"), //
        IOWRITEBYTESPERSEC("IO Write Bytes/sec"), //
        WORKINGSETPRIVATE("Working Set - Private"), //
        WORKINGSET("Working Set"), //
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
        // First element defines WMI instance name field and PDH instance filter
        NAME(TOTAL_OR_IDLE_INSTANCES),
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
}
