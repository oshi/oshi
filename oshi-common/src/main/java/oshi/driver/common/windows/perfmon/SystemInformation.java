/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

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
}
