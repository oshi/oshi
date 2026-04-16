/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.perfmon;

import static oshi.driver.common.windows.perfmon.PerfmonConstants.TOTAL_INSTANCE;

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
}
