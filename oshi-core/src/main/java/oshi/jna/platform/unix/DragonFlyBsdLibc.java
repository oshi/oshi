/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

import com.sun.jna.Pointer;
import com.sun.jna.platform.unix.LibCAPI.size_t;

/**
 * DragonFly BSD C library. Extends {@link FreeBsdLibc} since DragonFly shares the same sysctl and CPU tick interfaces;
 * its {@code struct utmpx} differs, so the inherited utmpx functions do not apply. Adds the DragonFly-specific
 * {@link #lwp_gettid()} thread ID function and {@link #sched_getaffinity(int, size_t, Pointer)}.
 */
public interface DragonFlyBsdLibc extends FreeBsdLibc {
    DragonFlyBsdLibc INSTANCE = BsdLibcLoader.loadLibc(DragonFlyBsdLibc.class);

    /**
     * Returns the light weight process thread identifier for the calling thread.
     *
     * @return The LWP thread ID. Returns -1 on error.
     */
    int lwp_gettid();

    /**
     * The size of DragonFly's {@code cpu_set_t}, a {@code cpumask_t} of four 64-bit words on x86_64.
     */
    int CPU_SET_SIZE = 32;

    /**
     * Gets the CPU affinity mask of a process, that of its first LWP.
     *
     * @param pid        the process ID, or 0 for the calling process
     * @param cpusetsize the size of the buffer, at most {@link #CPU_SET_SIZE} of which is written
     * @param mask       the buffer that receives the mask, CPU 0 in the lowest bit of the first word
     * @return 0 on success, -1 on error
     */
    int sched_getaffinity(int pid, size_t cpusetsize, Pointer mask);
}
