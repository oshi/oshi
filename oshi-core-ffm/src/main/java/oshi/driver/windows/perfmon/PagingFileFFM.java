/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PAGING_FILE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.ffm.util.platform.windows.PerfCounterQueryFFM;

/**
 * Utility to query Paging File performance counter using FFM.
 */
@ThreadSafe
public final class PagingFileFFM {

    private PagingFileFFM() {
    }

    /**
     * Returns paging file counters.
     *
     * @return Paging file counters for memory.
     */
    public static Map<PagingPercentProperty, Long> querySwapUsed() {
        return PerfCounterQueryFFM.queryValues(PagingPercentProperty.class, PAGING_FILE,
                WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE);
    }
}
