/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static oshi.jna.platform.unix.OpenBsdLibc.CP_IDLE;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_INTR;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_NICE;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_SYS;
import static oshi.jna.platform.unix.OpenBsdLibc.CP_USER;
import static oshi.jna.platform.unix.OpenBsdLibc.CTL_KERN;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_CPTIME;
import static oshi.jna.platform.unix.OpenBsdLibc.KERN_CPTIME2;

import java.util.Arrays;

import com.sun.jna.Memory;
import com.sun.jna.Native;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdCentralProcessor;
import oshi.jna.platform.unix.OpenBsdLibc;
import oshi.util.platform.unix.openbsd.OpenBsdSysctlUtil;

/**
 * OpenBSD Central Processor implementation, backed by JNA sysctl and native library calls.
 */
@ThreadSafe
public class OpenBsdCentralProcessorJNA extends OpenBsdCentralProcessor {

    @Override
    protected int sysctl(String name, int def) {
        return OpenBsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected String sysctl(String name, String def) {
        return OpenBsdSysctlUtil.sysctl(name, def);
    }

    @Override
    protected String sysctl(int[] mib, String def) {
        return OpenBsdSysctlUtil.sysctl(mib, def);
    }

    @Override
    protected int sysctl(int[] mib, int def) {
        return OpenBsdSysctlUtil.sysctl(mib, def);
    }

    /**
     * Get the system CPU load ticks
     *
     * @return The system CPU load ticks
     */
    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int[] mib = new int[2];
        mib[0] = CTL_KERN;
        mib[1] = KERN_CPTIME;
        try (Memory m = OpenBsdSysctlUtil.sysctl(mib)) {
            // array of 5 or 6 native longs
            long[] cpuTicks = cpTimeToTicks(m, false);
            if (cpuTicks.length >= 5) {
                ticks[TickType.USER.getIndex()] = cpuTicks[CP_USER];
                ticks[TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                ticks[TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                int offset = cpuTicks.length > 5 ? 1 : 0;
                ticks[TickType.IRQ.getIndex()] = cpuTicks[CP_INTR + offset];
                ticks[TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE + offset];
            }
        }
        return ticks;
    }

    /**
     * Get the processor CPU load ticks
     *
     * @return The processor CPU load ticks
     */
    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        int[] mib = new int[3];
        mib[0] = CTL_KERN;
        mib[1] = KERN_CPTIME2;
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            mib[2] = cpu;
            try (Memory m = OpenBsdSysctlUtil.sysctl(mib)) {
                // array of 5 or 6 longs
                long[] cpuTicks = cpTimeToTicks(m, true);
                if (cpuTicks.length >= 5) {
                    ticks[cpu][TickType.USER.getIndex()] = cpuTicks[CP_USER];
                    ticks[cpu][TickType.NICE.getIndex()] = cpuTicks[CP_NICE];
                    ticks[cpu][TickType.SYSTEM.getIndex()] = cpuTicks[CP_SYS];
                    int offset = cpuTicks.length > 5 ? 1 : 0;
                    ticks[cpu][TickType.IRQ.getIndex()] = cpuTicks[CP_INTR + offset];
                    ticks[cpu][TickType.IDLE.getIndex()] = cpuTicks[CP_IDLE + offset];
                }
            }
        }
        return ticks;
    }

    /**
     * Parse memory buffer returned from sysctl kern.cptime or kern.cptime2 to an array of 5 or 6 longs depending on
     * version.
     * <p>
     * Versions 6.4 and later have a 6-element array while earlier versions have only 5 elements. Additionally
     * kern.cptime uses a native-sized long (32- or 64-bit) value while kern.cptime2 is always a 64-bit value.
     *
     * @param m          A buffer containing the array.
     * @param force64bit True if the buffer is filled with 64-bit longs, false if native long sized values
     * @return The array
     */
    private static long[] cpTimeToTicks(Memory m, boolean force64bit) {
        long longBytes = force64bit ? 8L : Native.LONG_SIZE;
        int arraySize = m == null ? 0 : (int) (m.size() / longBytes);
        if (force64bit && m != null) {
            return m.getLongArray(0, arraySize);
        }
        long[] ticks = new long[arraySize];
        for (int i = 0; i < arraySize; i++) {
            ticks[i] = m.getNativeLong(i * longBytes).longValue();
        }
        return ticks;
    }

    /**
     * Returns the system load average for the number of elements specified, up to 3, representing 1, 5, and 15 minutes.
     * The system load average is the sum of the number of runnable entities queued to the available processors and the
     * number of runnable entities running on the available processors averaged over a period of time. The way in which
     * the load average is calculated is operating system specific but is typically a damped time-dependent average. If
     * the load average is not available, a negative value is returned. This method is designed to provide a hint about
     * the system load and may be queried frequently.
     * <p>
     * The load average may be unavailable on some platforms (e.g., Windows) where it is expensive to implement this
     * method.
     *
     * @param nelem Number of elements to return.
     * @return an array of the system load averages for 1, 5, and 15 minutes with the size of the array specified by
     *         nelem; or negative values if not available.
     */
    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        int retval = OpenBsdLibc.INSTANCE.getloadavg(average, nelem);
        if (retval < nelem) {
            Arrays.fill(average, -1d);
        }
        return average;
    }
}
