/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PhysicalDisk;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.util.tuples.Pair;

/**
 * Utility to query PhysicalDisk performance counter
 */
@ThreadSafe
public final class PhysicalDiskJNA {

    private PhysicalDiskJNA() {
    }

    /**
     * Returns physical disk performance counters.
     *
     * @return Performance Counters for physical disks.
     */
    public static Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> queryDiskCounters() {
        return PhysicalDisk.queryDiskCounters(PerfCounterQueryExecutorJNA.INSTANCE);
    }
}
