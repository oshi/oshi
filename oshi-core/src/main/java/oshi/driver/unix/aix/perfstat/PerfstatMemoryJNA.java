/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_memory_total_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query performance stats for memory
 */
@ThreadSafe
public final class PerfstatMemoryJNA {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatMemoryJNA() {
    }

    /**
     * Queries perfstat_memory_total for total memory usage statistics
     *
     * @return usage statistics
     */
    public static perfstat_memory_total_t queryMemoryTotal() {
        perfstat_memory_total_t memory = new perfstat_memory_total_t();
        int ret = PERF.perfstat_memory_total(null, memory, memory.size(), 1);
        if (ret > 0) {
            return memory;
        }
        return new perfstat_memory_total_t();
    }
}
