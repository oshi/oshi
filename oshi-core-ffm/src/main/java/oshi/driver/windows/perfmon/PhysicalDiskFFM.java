/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PHYSICAL_DISK;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.ffm.util.platform.windows.PerfCounterWildcardQueryFFM;
import oshi.util.tuples.Pair;

/**
 * Utility to query PhysicalDisk performance counter using FFM.
 */
@ThreadSafe
public final class PhysicalDiskFFM {

    private PhysicalDiskFFM() {
    }

    /**
     * Returns physical disk performance counters.
     *
     * @return Performance Counters for physical disks.
     */
    public static Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> queryDiskCounters() {
        return PerfCounterWildcardQueryFFM.queryInstancesAndValues(PhysicalDiskProperty.class, PHYSICAL_DISK,
                WIN32_PERF_RAW_DATA_PERF_DISK_PHYSICAL_DISK_WHERE_NAME_NOT_TOTAL);
    }
}
