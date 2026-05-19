/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.MemoryInformation;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;

@ThreadSafe
public final class MemoryInformationFFM {
    private MemoryInformationFFM() {
    }

    public static Map<PageSwapProperty, Long> queryPageSwaps() {
        return MemoryInformation.queryPageSwaps(PerfCounterQueryExecutorFFM.INSTANCE);
    }
}
