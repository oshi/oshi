/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.driver.unix.aix.perfstat;

import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_ID_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_PROCESS_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_process;
import static oshi.ffm.unix.aix.PerfstatFunctions.procNumThreads;
import static oshi.ffm.unix.aix.PerfstatFunctions.procPid;
import static oshi.ffm.unix.aix.PerfstatFunctions.procRealInuse;
import static oshi.ffm.unix.aix.PerfstatFunctions.procRealMemData;
import static oshi.ffm.unix.aix.PerfstatFunctions.procRealMemText;
import static oshi.ffm.unix.aix.PerfstatFunctions.procScpuTime;
import static oshi.ffm.unix.aix.PerfstatFunctions.procUcpuTime;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * FFM-backed driver for {@code perfstat_process}, mirroring {@code oshi.driver.unix.aix.perfstat.PerfstatProcessJNA}.
 */
@ThreadSafe
public final class PerfstatProcessFFM {

    private PerfstatProcessFFM() {
    }

    /** POJO mirror of the {@code perfstat_process_t} fields OSHI consumes. */
    public static final class ProcessInfo {
        public long pid;
        public long num_threads;
        public long proc_real_mem_data;
        public long proc_real_mem_text;
        public long real_inuse;
        public double ucpu_time;
        public double scpu_time;
    }

    /** Slack added to the perfstat_process count to absorb new processes between count and fill calls. */
    private static final int PROC_COUNT_PAD = 10;

    /**
     * Queries {@code perfstat_process} for per-process statistics.
     * <p>
     * Pads the array by {@value #PROC_COUNT_PAD} entries past the initial count to absorb process churn between the two
     * perfstat calls — mirrors {@code PerfstatProcessJNA} and the {@code MacOperatingSystemJNA.getThreadCount}
     * precedent (+10).
     *
     * @return one {@link ProcessInfo} per process, or an empty array on error
     */
    public static ProcessInfo[] queryProcesses() {
        try (Arena arena = Arena.ofConfined()) {
            int count = perfstat_process(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_PROCESS_T_SIZE, 0);
            if (count <= 0) {
                return new ProcessInfo[0];
            }
            int padded = count + PROC_COUNT_PAD;
            MemorySegment buf = arena.allocate((long) PERFSTAT_PROCESS_T_SIZE * padded);
            MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
            int ret = perfstat_process(firstName, buf, PERFSTAT_PROCESS_T_SIZE, padded);
            if (ret <= 0) {
                return new ProcessInfo[0];
            }
            ProcessInfo[] result = new ProcessInfo[ret];
            for (int i = 0; i < ret; i++) {
                long off = (long) i * PERFSTAT_PROCESS_T_SIZE;
                ProcessInfo p = new ProcessInfo();
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
        } catch (Throwable t) {
            return new ProcessInfo[0];
        }
    }
}
