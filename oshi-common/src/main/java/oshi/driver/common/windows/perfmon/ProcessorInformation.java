/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.NOT_TOTAL_INSTANCES;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_INSTANCE;

import oshi.annotation.concurrent.ThreadSafe;

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
}
