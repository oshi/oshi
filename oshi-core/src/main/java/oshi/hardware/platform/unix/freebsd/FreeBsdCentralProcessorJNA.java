/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdCentralProcessor;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.FreeBsdLibc;
import oshi.jna.platform.unix.FreeBsdLibc.CpTime;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A CPU
 */
@ThreadSafe
public class FreeBsdCentralProcessorJNA extends FreeBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBsdCentralProcessorJNA.class);

    private static final long CPTIME_SIZE;
    static {
        try (CpTime cpTime = new CpTime()) {
            CPTIME_SIZE = cpTime.size();
        }
    }

    @Override
    protected String sysctlString(String name, String def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected long sysctlLong(String name, long def) {
        return BsdSysctlUtil.sysctl(name, def);
    }

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        try (CpTime cpTime = new CpTime()) {
            BsdSysctlUtil.sysctl("kern.cp_time", cpTime);
            ticks[TickType.USER.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_USER];
            ticks[TickType.NICE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_NICE];
            ticks[TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_SYS];
            ticks[TickType.IRQ.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_INTR];
            ticks[TickType.IDLE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_IDLE];
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = FreeBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            for (int i = Math.max(retval, 0); i < average.length; i++) {
                average[i] = -1d;
            }
        }
        return average;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];

        // Allocate memory for array of CPTime
        long arraySize = CPTIME_SIZE * getLogicalProcessorCount();
        try (Memory p = new Memory(arraySize);
                CloseableSizeTByReference oldlenp = new CloseableSizeTByReference(arraySize)) {
            String name = "kern.cp_times";
            // Fetch
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, oldlenp, null, size_t.ZERO)) {
                LOG.error("Failed sysctl call: {}, Error code: {}", name, Native.getLastError());
                return ticks;
            }
            // p now points to the data; need to copy each element
            for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                ticks[cpu][TickType.USER.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_USER * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.NICE.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_NICE * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.SYSTEM.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_SYS * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.IRQ.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_INTR * FreeBsdLibc.UINT64_SIZE); // lgtm
                ticks[cpu][TickType.IDLE.getIndex()] = p
                        .getLong(CPTIME_SIZE * cpu + FreeBsdLibc.CP_IDLE * FreeBsdLibc.UINT64_SIZE); // lgtm
            }
        }
        return ticks;
    }

    @Override
    public long queryContextSwitches() {
        String name = "vm.stats.sys.v_swtch";
        size_t.ByReference size = new size_t.ByReference(new size_t(FreeBsdLibc.INT_SIZE));
        try (Memory p = new Memory(size.longValue())) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                return 0L;
            }
            return ParseUtil.unsignedIntToLong(p.getInt(0));
        }
    }

    @Override
    public long queryInterrupts() {
        String name = "vm.stats.sys.v_intr";
        size_t.ByReference size = new size_t.ByReference(new size_t(FreeBsdLibc.INT_SIZE));
        try (Memory p = new Memory(size.longValue())) {
            if (0 != FreeBsdLibc.INSTANCE.sysctlbyname(name, p, size, null, size_t.ZERO)) {
                return 0L;
            }
            return ParseUtil.unsignedIntToLong(p.getInt(0));
        }
    }
}
