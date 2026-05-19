/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.MemoryInformation;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;

/**
 * Utility to query Memory performance counter
 */
@ThreadSafe
public final class MemoryInformationJNA {

    private MemoryInformationJNA() {
    }

    /**
     * Returns page swap counters
     *
     * @return Page swap counters for memory.
     */
    public static Map<PageSwapProperty, Long> queryPageSwaps() {
        return MemoryInformation.queryPageSwaps(PerfCounterQueryExecutorJNA.INSTANCE);
    }
}
