/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.MEMORY;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_MEMORY;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Memory performance counter enums
 */
@ThreadSafe
public final class MemoryInformation {

    /**
     * For pages in/out
     */
    public enum PageSwapProperty implements PdhCounterProperty {
        /** Pages read from disk per second. */
        PAGESINPUTPERSEC(null, "Pages Input/sec"),
        /** Pages written to disk per second. */
        PAGESOUTPUTPERSEC(null, "Pages Output/sec");

        private final String instance;
        private final String counter;

        PageSwapProperty(String instance, String counter) {
            this.instance = instance;
            this.counter = counter;
        }

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getCounter() {
            return counter;
        }
    }

    private MemoryInformation() {
    }

    /**
     * Returns page swap counters.
     *
     * @param executor the performance counter query executor
     * @return Page swap counters for memory, or empty map if OS counters are disabled.
     */
    public static Map<PageSwapProperty, Long> queryPageSwaps(PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(PageSwapProperty.class, MEMORY, WIN32_PERF_RAW_DATA_PERF_OS_MEMORY);
    }
}
