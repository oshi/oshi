/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.platform.unix.freebsd.FreeBsdLibcFunctions;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor;
import oshi.util.ParseUtil;

/**
 * FFM-backed FreeBSD central processor.
 */
@ThreadSafe
public class FreeBsdCentralProcessorFFM extends FreeBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessorFFM.class);

    // FreeBSD CPU state indices into kern.cp_time / kern.cp_times arrays
    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;
    private static final int CPUSTATES = 5;
    private static final long UINT64_SIZE = Long.BYTES;
    private static final long CPTIME_SIZE = CPUSTATES * UINT64_SIZE;

    @Override
    protected String sysctlString(String name, String def) {
        return BsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    protected long sysctlLong(String name, long def) {
        return BsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cpTime = arena.allocate(CPTIME_SIZE);
            if (BsdSysctlUtilFFM.sysctl("kern.cp_time", cpTime)) {
                ticks[TickType.USER.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_USER);
                ticks[TickType.NICE.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_NICE);
                ticks[TickType.SYSTEM.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_SYS);
                ticks[TickType.IRQ.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_INTR);
                ticks[TickType.IDLE.getIndex()] = cpTime.getAtIndex(JAVA_LONG, CP_IDLE);
            }
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        final int n = nelem;
        return callInArenaOrDefault(arena -> {
            MemorySegment avg = arena.allocate(JAVA_DOUBLE, n);
            int retval = FreeBsdLibcFunctions.getloadavg(avg, n);
            double[] result = new double[n];
            for (int i = 0; i < n; i++) {
                result[i] = (i < retval) ? avg.getAtIndex(JAVA_DOUBLE, i) : -1d;
            }
            return result;
        }, LOG, Level.WARN, "Failed to read load average", fillNegative(nelem));
    }

    private static double[] fillNegative(int nelem) {
        double[] arr = new double[nelem];
        for (int i = 0; i < nelem; i++) {
            arr[i] = -1d;
        }
        return arr;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        int cpus = getLogicalProcessorCount();
        long[][] ticks = new long[cpus][TickType.values().length];
        MemorySegment buf = BsdSysctlUtilFFM.sysctl("kern.cp_times");
        if (buf == null) {
            return ticks;
        }
        for (int cpu = 0; cpu < cpus; cpu++) {
            long base = CPTIME_SIZE * cpu;
            ticks[cpu][TickType.USER.getIndex()] = buf.get(JAVA_LONG, base + CP_USER * UINT64_SIZE);
            ticks[cpu][TickType.NICE.getIndex()] = buf.get(JAVA_LONG, base + CP_NICE * UINT64_SIZE);
            ticks[cpu][TickType.SYSTEM.getIndex()] = buf.get(JAVA_LONG, base + CP_SYS * UINT64_SIZE);
            ticks[cpu][TickType.IRQ.getIndex()] = buf.get(JAVA_LONG, base + CP_INTR * UINT64_SIZE);
            ticks[cpu][TickType.IDLE.getIndex()] = buf.get(JAVA_LONG, base + CP_IDLE * UINT64_SIZE);
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        return ParseUtil.unsignedIntToLong(BsdSysctlUtilFFM.sysctl("vm.stats.sys.v_swtch", 0));
    }

    @Override
    public long queryInterrupts() {
        return ParseUtil.unsignedIntToLong(BsdSysctlUtilFFM.sysctl("vm.stats.sys.v_intr", 0));
    }
}
