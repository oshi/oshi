/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.GPU_ADAPTER_MEMORY;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.GPU_ENGINE;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.ffm.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.util.tuples.Pair;

/**
 * Utility to query GPU Engine and GPU Adapter Memory performance counters using FFM. Available on Windows 10 version
 * 1709 and later.
 */
@ThreadSafe
public final class GpuInformationFFM {

    private GpuInformationFFM() {
    }

    /**
     * Queries GPU Engine running time counters for all instances.
     *
     * @return pair of instance name list and counter value map
     */
    public static Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(GpuEngineProperty.class, GPU_ENGINE);
    }

    /**
     * Queries GPU Adapter Memory counters for all instances.
     *
     * @return pair of instance name list and counter value map
     */
    public static Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValuesFromPDH(GpuAdapterMemoryProperty.class,
                GPU_ADAPTER_MEMORY);
    }
}
