/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.MEMORY;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_MEMORY;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.util.platform.windows.PerfCounterQuery;

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
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(PageSwapProperty.class, MEMORY, WIN32_PERF_RAW_DATA_PERF_OS_MEMORY);
    }
}
