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
}
