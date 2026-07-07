/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.netbsd;

import static oshi.jna.platform.unix.NetBsdLibc.CPUSTATES;
import static oshi.jna.platform.unix.NetBsdLibc.CP_IDLE;
import static oshi.jna.platform.unix.NetBsdLibc.CP_INTR;
import static oshi.jna.platform.unix.NetBsdLibc.CP_NICE;
import static oshi.jna.platform.unix.NetBsdLibc.CP_SYS;
import static oshi.jna.platform.unix.NetBsdLibc.CP_USER;
import static oshi.jna.platform.unix.NetBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.NetBsdLibc.KERN_CP_TIME;

import java.util.Arrays;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.netbsd.NetBsdCentralProcessor;
import oshi.jna.platform.unix.NetBsdLibc;
import oshi.util.platform.unix.netbsd.NetBsdSysctlUtil;

/**
 * A CentralProcessor for NetBSD that uses JNA native sysctl calls when the JNA native library is available, falling
 * back to the command-line {@link NetBsdCentralProcessor} implementation otherwise.
 */
@ThreadSafe
public class NetBsdCentralProcessorJNA extends NetBsdCentralProcessor {

    @Override
    protected long[] querySystemCpuLoadTicks() {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.querySystemCpuLoadTicks();
        }
        long[] ticks = new long[TickType.values().length];
        int[] mib = { CTL_KERN, KERN_CP_TIME };
        try (Memory m = NetBsdSysctlUtil.sysctl(mib)) {
            if (m != null) {
                long[] cpuTicks = cpTimeToTicks(m);
                if (cpuTicks.length >= CPUSTATES) {
                    ticks[TickType.USER.getIndex()] = cpuTicks[CP_USER];
                    ticks[TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                    ticks[TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                    ticks[TickType.IRQ.getIndex()] = cpuTicks[CP_INTR];
                    ticks[TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE];
                }
            }
        }
        return ticks;
    }

    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.queryProcessorCpuLoadTicks();
        }
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        // On NetBSD, kern.cp_time with a third MIB element gives per-CPU data
        int[] mib = { CTL_KERN, KERN_CP_TIME, 0 };
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            mib[2] = cpu;
            try (Memory m = NetBsdSysctlUtil.sysctl(mib)) {
                if (m != null) {
                    long[] cpuTicks = cpTimeToTicks(m);
                    if (cpuTicks.length >= CPUSTATES) {
                        ticks[cpu][TickType.USER.getIndex()] = cpuTicks[CP_USER];
                        ticks[cpu][TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                        ticks[cpu][TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                        ticks[cpu][TickType.IRQ.getIndex()] = cpuTicks[CP_INTR];
                        ticks[cpu][TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE];
                    }
                }
            }
        }
        return ticks;
    }

    /**
     * Parse memory buffer returned from sysctl kern.cp_time to an array of longs representing CPU states.
     *
     * @param m A buffer containing the array of 64-bit values.
     * @return The array
     */
    private static long[] cpTimeToTicks(Memory m) {
        int arraySize = (int) (m.size() / 8L);
        return m.getLongArray(0, arraySize);
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (!NetBsdSysctlUtil.JNA_AVAILABLE) {
            return super.getSystemLoadAverage(nelem);
        }
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = NetBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }
}
