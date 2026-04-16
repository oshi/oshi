/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * GPU performance counter enums. Available on Windows 10 version 1709 and later.
 */
@ThreadSafe
public final class GpuInformation {

    /**
     * GPU Engine running time counter properties. Instance names have the form:
     * {@code pid_<PID>_luid_0x<HIGH>_0x<LOW>_phys_0_eng_<N>_engtype_<TYPE>}
     */
    public enum GpuEngineProperty implements PdhCounterWildcardProperty {
        // First element: instance filter (all instances)
        NAME("*"),
        // Running time in 100ns units (raw cumulative counter)
        RUNNING_TIME("Running Time"),
        // Total elapsed time in 100ns units (SecondValue of Running Time counter; idle = base - active)
        RUNNING_TIME_BASE("Running Time_Base");

        private final String counter;

        GpuEngineProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    /**
     * GPU Adapter Memory counter properties. Instance names have the form: {@code luid_0x<HIGH>_0x<LOW>_phys_0}
     */
    public enum GpuAdapterMemoryProperty implements PdhCounterWildcardProperty {
        // First element: instance filter (all instances)
        NAME("*"), DEDICATED_USAGE("Dedicated Usage"), SHARED_USAGE("Shared Usage");

        private final String counter;

        GpuAdapterMemoryProperty(String counter) {
            this.counter = counter;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private GpuInformation() {
    }
}
