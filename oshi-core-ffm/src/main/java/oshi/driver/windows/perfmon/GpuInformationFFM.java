/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.GpuInformation;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class GpuInformationFFM {
    private GpuInformationFFM() {
    }

    public static Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters() {
        return GpuInformation.queryGpuEngineCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }

    public static Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters() {
        return GpuInformation.queryGpuAdapterMemoryCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
