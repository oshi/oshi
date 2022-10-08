/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.MEMORY;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_MEMORY;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty;

/**
 * Utility to query Memory performance counter
 */
@ThreadSafe
public final class MemoryInformation {

    /**
     * For pages in/out
     */
    public enum PageSwapProperty implements PdhCounterProperty {
        PAGESINPUTPERSEC(null, "Pages Input/sec"), //
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
