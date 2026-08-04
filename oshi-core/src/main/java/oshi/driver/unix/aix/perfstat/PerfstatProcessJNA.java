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

    /** Minimum slack added to the perfstat_process count to absorb new processes between count and fill calls. */
    private static final int MIN_PROC_COUNT_PAD = 64;

    /** Slack is at least one count in this many, so the headroom does not thin out on large systems. */
    private static final int PROC_COUNT_PAD_DIVISOR = 10;

    /** Bound on re-counting when the padded buffer still filled exactly. */
    private static final int MAX_BUFFER_RETRIES = 3;

    /**
     * Returns the array size to allocate for a reported process count.
     * <p>
     * The slack absorbs processes spawned between the count and fill calls, which tracks the system's spawn rate rather
     * than its process count — and the two pull in opposite directions. A small, busy host churns faster than a
     * proportional pad would cover, so a fixed floor carries that case; a large host makes that same floor thin, so a
     * proportional term carries that one. Take whichever is larger.
     *
     * @param count the process count perfstat reported
     * @return the number of entries to allocate
     */
    private static int paddedSize(int count) {
        return count + Math.max(MIN_PROC_COUNT_PAD, count / PROC_COUNT_PAD_DIVISOR);
    }

    /**
     * Queries perfstat_process for per-process usage statistics.
     * <p>
     * The two-call pattern (count then fill) leaves a window in which a process can spawn between the calls. perfstat
     * returns its array sorted by pid; if we allocate exactly {@code count} entries and a new process appears, the
     * highest-pid process (often us, the JVM) gets cut off the tail — observed on a busy shared build host as
     * {@code getProcess(getProcessId())} returning null.
     * <p>
     * The allocation is padded by {@link #paddedSize(int)} to absorb ordinary churn, but padding alone is a guess: a
     * return equal to the allocation means the buffer filled exactly and the tail may have been dropped, so re-count
     * and retry up to {@value #MAX_BUFFER_RETRIES} times. After that the result is returned as-is, which is still
     * better than discarding it.
     *
     * @return an array of usage statistics
     */
    public static AixPerfstatProcess[] queryProcesses() {
        perfstat_process_t process = new perfstat_process_t();
        // With null, null, ..., 0, returns total # of elements
        int procCount = PERF.perfstat_process(null, null, process.size(), 0);
        for (int attempt = 0; procCount > 0 && attempt <= MAX_BUFFER_RETRIES; attempt++) {
            int padded = paddedSize(procCount);
            perfstat_process_t[] proct = (perfstat_process_t[]) process.toArray(padded);
            perfstat_id_t firstprocess = new perfstat_id_t(); // name is ""
            int ret = PERF.perfstat_process(firstprocess, proct, process.size(), padded);
            if (ret <= 0) {
                break;
            }
            if (ret == padded && attempt < MAX_BUFFER_RETRIES) {
                // The buffer was filled exactly, so processes beyond it may have been dropped. Re-count and retry
                // rather than return a list that is silently missing its tail. If the re-count itself fails there is
                // nothing better to try, so keep the full buffer already read rather than discarding it for an empty
                // result.
                int recount = PERF.perfstat_process(null, null, process.size(), 0);
                if (recount > 0) {
                    procCount = recount;
                    continue;
                }
            }
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
        return new AixPerfstatProcess[0];
    }
}
