/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemorySegment;

import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.CentralProcessor;
import oshi.hardware.platform.unix.freebsd.FreeBsdCentralProcessorFFM;

/**
 * DragonFly BSD Central Processor (FFM). Inherits {@code kern.cp_time} system tick logic from
 * {@link FreeBsdCentralProcessorFFM} and overrides per-CPU tick retrieval to use DragonFly's {@code kern.cputime}
 * sysctl (FreeBSD uses {@code kern.cp_times}).
 */
public class DragonFlyBsdCentralProcessorFFM extends FreeBsdCentralProcessorFFM {

    private static final int CP_USER = 0;
    private static final int CP_NICE = 1;
    private static final int CP_SYS = 2;
    private static final int CP_INTR = 3;
    private static final int CP_IDLE = 4;
    private static final long UINT64_SIZE = Long.BYTES;

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        int cpus = getLogicalProcessorCount();
        long[][] ticks = new long[cpus][CentralProcessor.TickType.values().length];
        // DragonFly's kern.cputime returns a packed per-CPU array; the struct size per CPU is the total length
        // divided by CPU count (the trailing fields can vary by DragonFly version).
        MemorySegment buf = BsdSysctlUtilFFM.sysctl("kern.cputime");
        if (buf == null) {
            return ticks;
        }
        long arraySize = buf.byteSize();
        if (arraySize <= 0) {
            return ticks;
        }
        long structSize = arraySize / cpus;
        for (int cpu = 0; cpu < cpus; cpu++) {
            long base = structSize * cpu;
            ticks[cpu][TickType.USER.getIndex()] = buf.get(JAVA_LONG, base + CP_USER * UINT64_SIZE);
            ticks[cpu][TickType.NICE.getIndex()] = buf.get(JAVA_LONG, base + CP_NICE * UINT64_SIZE);
            ticks[cpu][TickType.SYSTEM.getIndex()] = buf.get(JAVA_LONG, base + CP_SYS * UINT64_SIZE);
            ticks[cpu][TickType.IRQ.getIndex()] = buf.get(JAVA_LONG, base + CP_INTR * UINT64_SIZE);
            ticks[cpu][TickType.IDLE.getIndex()] = buf.get(JAVA_LONG, base + CP_IDLE * UINT64_SIZE);
        }
        return ticks;
    }
}
