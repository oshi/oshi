/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PagingFile;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;

/**
 * Utility to query PagingFile performance counter
 */
@ThreadSafe
public final class PagingFileJNA {

    private PagingFileJNA() {
    }

    /**
     * Returns paging file usage counters.
     *
     * @return Paging file counters.
     */
    public static Map<PagingPercentProperty, Long> querySwapUsed() {
        return PagingFile.querySwapUsed(PerfCounterQueryExecutorJNA.INSTANCE);
    }
}
