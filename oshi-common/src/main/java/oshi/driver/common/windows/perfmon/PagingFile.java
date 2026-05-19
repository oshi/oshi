/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.PAGING_FILE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_INSTANCE;
import static oshi.driver.common.windows.perfmon.PerfmonConstants.WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE;

import java.util.Collections;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Paging File performance counter enums
 */
@ThreadSafe
public final class PagingFile {

    /**
     * For swap file usage
     */
    public enum PagingPercentProperty implements PdhCounterProperty {
        /** Percentage of paging file in use. */
        PERCENTUSAGE(TOTAL_INSTANCE, "% Usage");

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
     * Returns paging file usage counters.
     *
     * @param executor the performance counter query executor
     * @return Paging file usage, or empty map if OS counters are disabled.
     */
    public static Map<PagingPercentProperty, Long> querySwapUsed(PerfCounterQueryExecutor executor) {
        if (executor.isPerfOsDisabled()) {
            return Collections.emptyMap();
        }
        return executor.queryValues(PagingPercentProperty.class, PAGING_FILE, WIN32_PERF_RAW_DATA_PERF_OS_PAGING_FILE);
    }
}
