/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PagingFile;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;

@ThreadSafe
public final class PagingFileFFM {
    private PagingFileFFM() {
    }

    public static Map<PagingPercentProperty, Long> querySwapUsed() {
        return PagingFile.querySwapUsed(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
