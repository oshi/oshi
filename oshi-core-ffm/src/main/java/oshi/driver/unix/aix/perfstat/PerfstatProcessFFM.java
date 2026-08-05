/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.unix.aix.perfstat;

import static oshi.ffm.platform.unix.aix.PerfstatFunctions.PERFSTAT_ID_T_SIZE;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.PERFSTAT_PROCESS_T_SIZE;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.perfstat_process;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procNumThreads;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procPid;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procRealInuse;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procRealMemData;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procRealMemText;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procScpuTime;
import static oshi.ffm.platform.unix.aix.PerfstatFunctions.procUcpuTime;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.aix.AixPerfstatProcess;
import oshi.ffm.ForeignFunctions;
import oshi.util.LogLevel;

/**
 * FFM-backed driver for {@code perfstat_process}, mirroring {@code oshi.driver.unix.aix.perfstat.PerfstatProcessJNA}.
 */
@ThreadSafe
public final class PerfstatProcessFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfstatProcessFFM.class);

    private PerfstatProcessFFM() {
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
     * Queries {@code perfstat_process} for per-process statistics.
     * <p>
     * The two-call pattern (count then fill) leaves a window in which a process can spawn between the calls. perfstat
     * returns its array sorted by pid, so a buffer sized to the first count drops the highest-pid entries — often the
     * JVM itself, observed on a busy shared build host as {@code getProcess(getProcessId())} returning null.
     * <p>
     * The allocation is padded by {@link #paddedSize(int)} to absorb ordinary churn, but padding alone is a guess: a
     * return equal to the allocation means the buffer filled exactly and the tail may have been dropped, so re-count
     * and retry up to {@value #MAX_BUFFER_RETRIES} times. Mirrors {@code PerfstatProcessJNA}.
     *
     * @return one {@link AixPerfstatProcess} per process, or an empty array on error
     */
    public static AixPerfstatProcess[] queryProcesses() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            int count = perfstat_process(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_PROCESS_T_SIZE, 0);
            // Bounded by the retry check below, not here: the only path back to the top is its continue,
            // which requires attempt < MAX_BUFFER_RETRIES. Do not hoist that bound into this guard --
            // arriving here with attempt == MAX_BUFFER_RETRIES must fall through to the mapping below, not
            // exit and discard a buffer that was read successfully.
            for (int attempt = 0; count > 0; attempt++) {
                int padded = paddedSize(count);
                MemorySegment buf = arena.allocate((long) PERFSTAT_PROCESS_T_SIZE * padded);
                MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
                int ret = perfstat_process(firstName, buf, PERFSTAT_PROCESS_T_SIZE, padded);
                if (ret <= 0) {
                    break;
                }
                if (ret == padded && attempt < MAX_BUFFER_RETRIES) {
                    // The buffer was filled exactly, so processes beyond it may have been dropped. Re-count and
                    // retry rather than return a list that is silently missing its tail. If the re-count itself
                    // fails there is nothing better to try, so keep the full buffer already read rather than
                    // discarding it for an empty result.
                    int recount = perfstat_process(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_PROCESS_T_SIZE, 0);
                    if (recount > 0) {
                        count = recount;
                        continue;
                    }
                }
                AixPerfstatProcess[] result = new AixPerfstatProcess[ret];
                for (int i = 0; i < ret; i++) {
                    long off = (long) i * PERFSTAT_PROCESS_T_SIZE;
                    AixPerfstatProcess p = new AixPerfstatProcess();
                    p.pid = procPid(buf, off);
                    p.num_threads = procNumThreads(buf, off);
                    p.proc_real_mem_data = procRealMemData(buf, off);
                    p.proc_real_mem_text = procRealMemText(buf, off);
                    p.real_inuse = procRealInuse(buf, off);
                    p.ucpu_time = procUcpuTime(buf, off);
                    p.scpu_time = procScpuTime(buf, off);
                    result[i] = p;
                }
                return result;
            }
            return new AixPerfstatProcess[0];
        }, LOG, LogLevel.TRACE, "Failed to query process statistics", new AixPerfstatProcess[0]);
    }
}
