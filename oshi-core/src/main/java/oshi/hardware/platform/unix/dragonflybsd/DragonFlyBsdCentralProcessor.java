/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.platform.unix.LibCAPI.size_t;

import oshi.hardware.CentralProcessor;
import oshi.hardware.platform.unix.freebsd.FreeBsdCentralProcessorJNA;
import oshi.jna.ByRef.CloseableSizeTByReference;
import oshi.jna.platform.unix.DragonFlyBsdLibc;
import oshi.jna.platform.unix.FreeBsdLibc;

/**
 * DragonFly BSD Central Processor. Overrides CPU tick retrieval to use DragonFly's kern.cputime sysctl via
 * {@link DragonFlyBsdLibc#INSTANCE}.
 */
public class DragonFlyBsdCentralProcessor extends FreeBsdCentralProcessorJNA {

    private static final Logger LOG = LoggerFactory.getLogger(DragonFlyBsdCentralProcessor.class);

    @Override
    public long[] querySystemCpuLoadTicks() {
        long[] ticks = new long[TickType.values().length];
        FreeBsdLibc.CpTime cpTime = new FreeBsdLibc.CpTime();
        try (CloseableSizeTByReference size = new CloseableSizeTByReference(cpTime.size())) {
            if (0 == DragonFlyBsdLibc.INSTANCE.sysctlbyname("kern.cp_time", cpTime.getPointer(), size, null,
                    size_t.ZERO)) {
                cpTime.read();
                ticks[TickType.USER.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_USER];
                ticks[TickType.NICE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_NICE];
                ticks[TickType.SYSTEM.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_SYS];
                ticks[TickType.IRQ.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_INTR];
                ticks[TickType.IDLE.getIndex()] = cpTime.cpu_ticks[FreeBsdLibc.CP_IDLE];
            }
        }
        return ticks;
    }

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        long[][] ticks = new long[getLogicalProcessorCount()][CentralProcessor.TickType.values().length];
        String name = "kern.cputime";
        // Two-step sysctl: first get size, then fetch data
        try (CloseableSizeTByReference size = new CloseableSizeTByReference()) {
            if (0 != DragonFlyBsdLibc.INSTANCE.sysctlbyname(name, null, size, null, size_t.ZERO)) {
                LOG.debug("Failed sysctl size query: {}, Error code: {}", name, Native.getLastError());
                return ticks;
            }
            long arraySize = size.getValue().longValue();
            if (arraySize <= 0) {
                return ticks;
            }
            long structSize = arraySize / getLogicalProcessorCount();
            try (Memory m = new Memory(arraySize);
                    CloseableSizeTByReference dataSize = new CloseableSizeTByReference(arraySize)) {
                if (0 != DragonFlyBsdLibc.INSTANCE.sysctlbyname(name, m, dataSize, null, size_t.ZERO)) {
                    LOG.debug("Failed sysctl data query: {}, Error code: {}", name, Native.getLastError());
                    return ticks;
                }
                for (int cpu = 0; cpu < getLogicalProcessorCount(); cpu++) {
                    ticks[cpu][TickType.USER.getIndex()] = m
                            .getLong(structSize * cpu + FreeBsdLibc.CP_USER * FreeBsdLibc.UINT64_SIZE);
                    ticks[cpu][TickType.NICE.getIndex()] = m
                            .getLong(structSize * cpu + FreeBsdLibc.CP_NICE * FreeBsdLibc.UINT64_SIZE);
                    ticks[cpu][TickType.SYSTEM.getIndex()] = m
                            .getLong(structSize * cpu + FreeBsdLibc.CP_SYS * FreeBsdLibc.UINT64_SIZE);
                    ticks[cpu][TickType.IRQ.getIndex()] = m
                            .getLong(structSize * cpu + FreeBsdLibc.CP_INTR * FreeBsdLibc.UINT64_SIZE);
                    ticks[cpu][TickType.IDLE.getIndex()] = m
                            .getLong(structSize * cpu + FreeBsdLibc.CP_IDLE * FreeBsdLibc.UINT64_SIZE);
                }
            }
        }
        return ticks;
    }
}
