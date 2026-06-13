/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.openbsd;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CPUSTATES;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_IDLE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_INTR;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_NICE;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_SYS;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CP_USER;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.CTL_KERN;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_CPTIME;
import static oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions.KERN_CPTIME2;

import java.lang.foreign.MemorySegment;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.ForeignFunctions;
import oshi.ffm.platform.unix.openbsd.OpenBsdLibcFunctions;
import oshi.ffm.util.platform.unix.openbsd.OpenBsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.openbsd.OpenBsdCentralProcessor;

/**
 * OpenBSD Central Processor implementation, backed by FFM sysctl and native library calls.
 */
@ThreadSafe
public class OpenBsdCentralProcessorFFM extends OpenBsdCentralProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenBsdCentralProcessorFFM.class);

    @Override
    protected int sysctl(String name, int def) {
        return OpenBsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    protected String sysctl(String name, String def) {
        return OpenBsdSysctlUtilFFM.sysctl(name, def);
    }

    @Override
    protected String sysctl(int[] mib, String def) {
        return OpenBsdSysctlUtilFFM.sysctl(mib, def);
    }

    @Override
    protected int sysctl(int[] mib, int def) {
        return OpenBsdSysctlUtilFFM.sysctl(mib, def);
    }

    @Override
    protected long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        int[] mib = { CTL_KERN, KERN_CPTIME };
        MemorySegment buf = OpenBsdSysctlUtilFFM.sysctl(mib);
        if (buf != null) {
            int arraySize = (int) (buf.byteSize() / JAVA_LONG.byteSize());
            if (arraySize >= CPUSTATES) {
                ticks[TickType.USER.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_USER);
                ticks[TickType.NICE.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_NICE);
                ticks[TickType.SYSTEM.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_SYS);
                int offset = arraySize > CPUSTATES ? 1 : 0;
                ticks[TickType.IRQ.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_INTR + offset);
                ticks[TickType.IDLE.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_IDLE + offset);
            }
        }
        return ticks;
    }

    @Override
    protected long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][TickType.values().length];
        for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
            int[] mib = { CTL_KERN, KERN_CPTIME2, cpu };
            MemorySegment buf = OpenBsdSysctlUtilFFM.sysctl(mib);
            if (buf != null) {
                int arraySize = (int) (buf.byteSize() / JAVA_LONG.byteSize());
                if (arraySize >= CPUSTATES) {
                    ticks[cpu][TickType.USER.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_USER);
                    ticks[cpu][TickType.NICE.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_NICE);
                    ticks[cpu][TickType.SYSTEM.getIndex()] = buf.getAtIndex(JAVA_LONG, CP_SYS);
                    int offset = arraySize > CPUSTATES ? 1 : 0;
                    ticks[cpu][TickType.IRQ.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_INTR + offset);
                    ticks[cpu][TickType.IDLE.getIndex()] = buf.getAtIndex(JAVA_LONG, (long) CP_IDLE + offset);
                }
            }
        }
        return ticks;
    }

    @Override
    public double[] getSystemLoadAverage(int nelem) {
        if (nelem < 1 || nelem > 3) {
            throw new IllegalArgumentException("Must include from one to three elements.");
        }
        double[] average = new double[nelem];
        Arrays.fill(average, -1d);
        return ForeignFunctions.callInArenaOrDefault(arena -> {
            MemorySegment avg = arena.allocate(JAVA_DOUBLE, nelem);
            int retval = OpenBsdLibcFunctions.getloadavg(avg, nelem);
            double[] result = new double[nelem];
            for (int i = 0; i < nelem; i++) {
                result[i] = (i < retval) ? avg.getAtIndex(JAVA_DOUBLE, i) : -1d;
            }
            return result;
        }, LOG, Level.WARN, "Failed to read load average", average);
    }
}
