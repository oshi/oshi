/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.driver.unix.aix.perfstat;

import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_CPU_TOTAL_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_CPU_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.PERFSTAT_ID_T_SIZE;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuBusyStolenPurr;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuDevintrs;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuIdle;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuIdleStolenPurr;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuSoftintrs;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuSys;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalBusyStolenPurr;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalDevintrs;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalIdle;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalIdleStolenPurr;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalLoadavg;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalNcpus;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalProcessorHZ;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalPswitch;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalSoftintrs;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalSys;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalUser;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuTotalWait;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuUser;
import static oshi.ffm.unix.aix.PerfstatFunctions.cpuWait;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_cpu;
import static oshi.ffm.unix.aix.PerfstatFunctions.perfstat_cpu_total;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;

/**
 * FFM-backed driver for {@code perfstat_cpu_total} and {@code perfstat_cpu}, mirroring
 * {@code oshi.driver.unix.aix.perfstat.PerfstatCpuJNA}.
 */
@ThreadSafe
public final class PerfstatCpuFFM {

    private static final Logger LOG = LoggerFactory.getLogger(PerfstatCpuFFM.class);

    private PerfstatCpuFFM() {
    }

    /** POJO mirror of the {@code perfstat_cpu_total_t} fields OSHI consumes. */
    public static final class CpuTotal {
        public int ncpus;
        public long processorHZ;
        public long user;
        public long sys;
        public long idle;
        public long wait;
        public long pswitch;
        public long devintrs;
        public long softintrs;
        public long[] loadavg = new long[3];
        public long idle_stolen_purr;
        public long busy_stolen_purr;
    }

    /** POJO mirror of {@code perfstat_cpu_t} per-CPU fields OSHI consumes. */
    public static final class Cpu {
        public long user;
        public long sys;
        public long idle;
        public long wait;
        public long devintrs;
        public long softintrs;
        public long idle_stolen_purr;
        public long busy_stolen_purr;
    }

    /**
     * Queries {@code perfstat_cpu_total} for aggregate CPU statistics.
     *
     * @return populated {@link CpuTotal}, or an empty instance on error
     */
    public static CpuTotal queryCpuTotal() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            CpuTotal result = new CpuTotal();
            MemorySegment buf = arena.allocate(PERFSTAT_CPU_TOTAL_T_SIZE);
            int ret = perfstat_cpu_total(MemorySegment.NULL, buf, PERFSTAT_CPU_TOTAL_T_SIZE, 1);
            if (ret > 0) {
                result.ncpus = cpuTotalNcpus(buf);
                result.processorHZ = cpuTotalProcessorHZ(buf);
                result.user = cpuTotalUser(buf);
                result.sys = cpuTotalSys(buf);
                result.idle = cpuTotalIdle(buf);
                result.wait = cpuTotalWait(buf);
                result.pswitch = cpuTotalPswitch(buf);
                result.devintrs = cpuTotalDevintrs(buf);
                result.softintrs = cpuTotalSoftintrs(buf);
                for (int i = 0; i < 3; i++) {
                    result.loadavg[i] = cpuTotalLoadavg(buf, i);
                }
                result.idle_stolen_purr = cpuTotalIdleStolenPurr(buf);
                result.busy_stolen_purr = cpuTotalBusyStolenPurr(buf);
            }
            return result;
        }, LOG, Level.TRACE, "Failed to query CPU total", new CpuTotal());
    }

    /**
     * Queries {@code perfstat_cpu} for per-CPU statistics.
     *
     * @return one {@link Cpu} per logical processor, or an empty array on error
     */
    public static Cpu[] queryCpu() {
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            // First call with NULL buf to get count.
            int count = perfstat_cpu(MemorySegment.NULL, MemorySegment.NULL, PERFSTAT_CPU_T_SIZE, 0);
            if (count <= 0) {
                return new Cpu[0];
            }
            MemorySegment buf = arena.allocate((long) PERFSTAT_CPU_T_SIZE * count);
            MemorySegment firstName = arena.allocate(PERFSTAT_ID_T_SIZE);
            // perfstat_id_t with empty name selects "start from beginning"; allocate() zero-fills.
            int ret = perfstat_cpu(firstName, buf, PERFSTAT_CPU_T_SIZE, count);
            if (ret <= 0) {
                return new Cpu[0];
            }
            Cpu[] result = new Cpu[ret];
            for (int i = 0; i < ret; i++) {
                long off = (long) i * PERFSTAT_CPU_T_SIZE;
                Cpu c = new Cpu();
                c.user = cpuUser(buf, off);
                c.sys = cpuSys(buf, off);
                c.idle = cpuIdle(buf, off);
                c.wait = cpuWait(buf, off);
                c.devintrs = cpuDevintrs(buf, off);
                c.softintrs = cpuSoftintrs(buf, off);
                c.idle_stolen_purr = cpuIdleStolenPurr(buf, off);
                c.busy_stolen_purr = cpuBusyStolenPurr(buf, off);
                result[i] = c;
            }
            return result;
        }, LOG, Level.TRACE, "Failed to query per-CPU statistics", new Cpu[0]);
    }

    /**
     * Returns the affinity mask derived from the number of CPUs reported by {@code perfstat_cpu_total}.
     *
     * @return mask, or {@code -1L} if {@code ncpus &gt; 63}
     */
    public static long queryCpuAffinityMask() {
        int cpus = queryCpuTotal().ncpus;
        if (cpus < 63) {
            return (1L << cpus) - 1;
        }
        return cpus == 63 ? Long.MAX_VALUE : -1L;
    }
}
