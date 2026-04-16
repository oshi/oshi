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
        // First element defines WMI instance name field and PDH instance filter
        NAME(NOT_TOTAL_INSTANCES),
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
        NAME(NOT_TOTAL_INSTANCES),
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
        // First element defines WMI instance name field and PDH instance filter
        NAME(NOT_TOTAL_INSTANCES),
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

    /**
     * System performance counters
     */
    public enum SystemTickCountProperty implements PdhCounterProperty {
        PERCENTDPCTIME(TOTAL_INSTANCE, "% DPC Time"), //
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
