/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PhysicalDisk;
import oshi.driver.common.windows.perfmon.PhysicalDisk.PhysicalDiskProperty;
import oshi.util.tuples.Pair;

@ThreadSafe
public final class PhysicalDiskFFM {
    private PhysicalDiskFFM() {
    }

    public static Pair<List<String>, Map<PhysicalDiskProperty, List<Long>>> queryDiskCounters() {
        return PhysicalDisk.queryDiskCounters(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
