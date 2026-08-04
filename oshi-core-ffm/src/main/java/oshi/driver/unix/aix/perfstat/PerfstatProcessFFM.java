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

    /** Slack added to the perfstat_process count to absorb new processes between count and fill calls. */
    private static final int PROC_COUNT_PAD = 64;

    /** Bound on re-counting when the padded buffer still filled exactly. */
    private static final int MAX_BUFFER_RETRIES = 3;

    /**
     * Queries {@code perfstat_process} for per-process statistics.
     * <p>
     * The two-call pattern (count then fill) leaves a window in which a process can spawn between the calls. perfstat
     * returns its array sorted by pid, so a buffer sized to the first count drops the highest-pid entries — often the
     * JVM itself, observed on a busy shared build host as {@code getProcess(getProcessId())} returning null.
     * <p>
     * The allocation is padded by {@value #PROC_COUNT_PAD} to absorb ordinary churn, but padding alone is a guess: a
     * return equal to the allocation means the buffer filled exactly and the tail may have been dropped, so re-count
     * and retry up to {@value #MAX_BUFFER_RETRIES} times. Mirrors {@code PerfstatProcessJNA}.
     *
     * @return one {@link AixPerfstatProcess} per process, or an empty array on error
     */
    public static AixPerfstatProcess[] queryProcesses() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            int count = perfstat_process(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_PROCESS_T_SIZE, 0);
            for (int attempt = 0; count > 0 && attempt <= MAX_BUFFER_RETRIES; attempt++) {
                int padded = count + PROC_COUNT_PAD;
                MemorySegment buf = arena.allocate((long) PERFSTAT_PROCESS_T_SIZE * padded);
                MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
                int ret = perfstat_process(firstName, buf, PERFSTAT_PROCESS_T_SIZE, padded);
                if (ret <= 0) {
                    break;
                }
                if (ret == padded && attempt < MAX_BUFFER_RETRIES) {
                    // The buffer was filled exactly, so processes beyond it may have been dropped. Re-count and
                    // retry rather than return a list that is silently missing its tail.
                    count = perfstat_process(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_PROCESS_T_SIZE, 0);
                    continue;
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
