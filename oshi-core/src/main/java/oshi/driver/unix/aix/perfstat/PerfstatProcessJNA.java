/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import com.sun.jna.platform.unix.aix.Perfstat;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_id_t;
import com.sun.jna.platform.unix.aix.Perfstat.perfstat_process_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPerfstatProcess;

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
    public static AixPerfstatProcess[] queryProcesses() {
        perfstat_process_t process = new perfstat_process_t();
        // With null, null, ..., 0, returns total # of elements
        int procCount = PERF.perfstat_process(null, null, process.size(), 0);
        if (procCount > 0) {
            int padded = procCount + PROC_COUNT_PAD;
            perfstat_process_t[] proct = (perfstat_process_t[]) process.toArray(padded);
            perfstat_id_t firstprocess = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_process(firstprocess, proct, process.size(), padded);
            if (ret > 0) {
                AixPerfstatProcess[] result = new AixPerfstatProcess[ret];
                for (int i = 0; i < ret; i++) {
                    perfstat_process_t stat = proct[i];
                    AixPerfstatProcess p = new AixPerfstatProcess();
                    p.pid = stat.pid;
                    p.num_threads = stat.num_threads;
                    p.proc_real_mem_data = stat.proc_real_mem_data;
                    p.proc_real_mem_text = stat.proc_real_mem_text;
                    p.real_inuse = stat.real_inuse;
                    p.ucpu_time = stat.ucpu_time;
                    p.scpu_time = stat.scpu_time;
                    result[i] = p;
                }
                return result;
            }
        }
        return new AixPerfstatProcess[0];
    }
}
