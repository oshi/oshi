/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import java.util.Arrays;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_id_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_process_t;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Utility to query performance stats for processes
 */
@ThreadSafe
public final class PerfstatProcess {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatProcess() {
    }

    /**
     * Queries perfstat_process for per-process usage statistics
     *
     * @return an array of usage statistics
     */
    public static perfstat_process_t[] queryProcesses() {
        perfstat_process_t process = new perfstat_process_t();
        // With null, null, ..., 0, returns total # of elements
        int procCount = PERF.perfstat_process(null, null, process.size(), 0);
        if (procCount > 0) {
            perfstat_process_t[] proct = (perfstat_process_t[]) process.toArray(procCount);
            perfstat_id_t firstprocess = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_process(firstprocess, proct, process.size(), procCount);
            if (ret > 0) {
                return Arrays.copyOf(proct, ret);
            }
        }
        return new perfstat_process_t[0];
    }
}
