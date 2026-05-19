/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.GPU_ADAPTER_MEMORY;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.GPU_ENGINE;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.tuples.Pair;

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
        /** Instance filter (all instances). */
        NAME("*"),
        /** Running time in 100ns units (raw cumulative counter). */
        RUNNING_TIME("Running Time"),
        /** Total elapsed time in 100ns units (SecondValue of Running Time counter; idle = base - active). */
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
        /** Instance filter (all instances). */
        NAME("*"),
        /** Dedicated GPU memory usage in bytes. */
        DEDICATED_USAGE("Dedicated Usage"),
        /** Shared GPU memory usage in bytes. */
        SHARED_USAGE("Shared Usage");

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

    /**
     * Queries GPU Engine counters for all instances.
     *
     * @param executor the performance counter query executor
     * @return pair of instance name list and counter value map
     */
    public static Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters(
            PerfCounterQueryExecutor executor) {
        return executor.queryInstancesAndValuesFromPDH(GpuEngineProperty.class, GPU_ENGINE);
    }

    /**
     * Queries GPU Adapter Memory counters for all instances.
     *
     * @param executor the performance counter query executor
     * @return pair of instance name list and counter value map
     */
    public static Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters(
            PerfCounterQueryExecutor executor) {
        return executor.queryInstancesAndValuesFromPDH(GpuAdapterMemoryProperty.class, GPU_ADAPTER_MEMORY);
    }
}
