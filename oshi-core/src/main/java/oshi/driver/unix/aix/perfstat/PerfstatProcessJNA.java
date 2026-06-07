/*
 * Copyright 2020-2026 The OSHI Project Contributors
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
public final class PerfstatProcessJNA {

    private static final Perfstat PERF = Perfstat.INSTANCE;

    private PerfstatProcessJNA() {
    }

    /** Slack added to the perfstat_process count to absorb new processes between count and fill calls. */
    private static final int PROC_COUNT_PAD = 10;

    /**
     * Queries perfstat_process for per-process usage statistics.
     * <p>
     * The two-call pattern (count then fill) leaves a window in which a process can spawn between the calls. perfstat
     * returns its array sorted by pid; if we allocate exactly {@code count} entries and a new process appears, the
     * highest-pid process (often us, the JVM) gets cut off the tail. Pad the allocation by {@value #PROC_COUNT_PAD} to
     * absorb churn — same pattern as {@code MacOperatingSystemJNA.getThreadCount} (+10).
     *
     * @return an array of usage statistics
     */
    public static perfstat_process_t[] queryProcesses() {
        perfstat_process_t process = new perfstat_process_t();
        // With null, null, ..., 0, returns total # of elements
        int procCount = PERF.perfstat_process(null, null, process.size(), 0);
        if (procCount > 0) {
            int padded = procCount + PROC_COUNT_PAD;
            perfstat_process_t[] proct = (perfstat_process_t[]) process.toArray(padded);
            perfstat_id_t firstprocess = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_process(firstprocess, proct, process.size(), padded);
            if (ret > 0) {
                return Arrays.copyOf(proct, ret);
            }
        }
        return new perfstat_process_t[0];
    }
}
