/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterWildcardQuery;
import oshi.util.platform.windows.PerfCounterWildcardQuery.PdhCounterWildcardProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query GPU Engine and GPU Adapter Memory performance counters. Available on Windows 10 version 1709 and
 * later.
 */
@ThreadSafe
public final class GpuInformation {

    static final String GPU_ENGINE = "GPU Engine";
    static final String GPU_ADAPTER_MEMORY = "GPU Adapter Memory";

    /**
     * GPU Engine running time counter properties. Instance names have the form:
     * {@code pid_<PID>_luid_0x<HIGH>_0x<LOW>_phys_0_eng_<N>_engtype_<TYPE>}
     */
    public enum GpuEngineProperty implements PdhCounterWildcardProperty {
        // First element: instance filter (all instances)
        NAME("*"),
        // Running time in 100ns units (raw cumulative counter)
        RUNNING_TIME("Running Time");

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

    /**
     * Queries GPU Engine running time counters for all instances.
     *
     * @return pair of instance name list and counter value map
     */
    public static Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(GpuEngineProperty.class, GPU_ENGINE);
    }

    /**
     * Queries GPU Adapter Memory counters for all instances.
     *
     * @return pair of instance name list and counter value map
     */
    public static Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return new Pair<>(Collections.emptyList(), Collections.emptyMap());
        }
        return PerfCounterWildcardQuery.queryInstancesAndValuesFromPDH(GpuAdapterMemoryProperty.class,
                GPU_ADAPTER_MEMORY);
    }
}
