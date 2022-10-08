/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.perfmon;

import static oshi.driver.windows.perfmon.PerfmonConstants.PAGING_FILE;
import static oshi.driver.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.PerfCounterQuery;
import oshi.util.platform.windows.PerfCounterQuery.PdhCounterProperty;

/**
 * Utility to query Paging File performance counter
 */
@ThreadSafe
public final class PagingFile {

    /**
     * For swap file usage
     */
    public enum PagingPercentProperty implements PdhCounterProperty {
        PERCENTUSAGE(PerfCounterQuery.TOTAL_INSTANCE, "% Usage");

        private final String instance;
        private final String counter;

        PagingPercentProperty(String instance, String counter) {
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

    private PagingFile() {
    }

    /**
     * Returns paging file counters
     *
     * @return Paging file counters for memory.
     */
    public static Map<PagingPercentProperty, Long> querySwapUsed() {
        if (PerfmonDisabled.PERF_OS_DISABLED) {
            return Collections.emptyMap();
        }
        return PerfCounterQuery.queryValues(PagingPercentProperty.class, PAGING_FILE,
                WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE);
    }
}
