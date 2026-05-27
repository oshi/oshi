/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.unix;

/**
 * DragonFly BSD C library. Extends {@link FreeBsdLibc} since DragonFly shares the same sysctl, utmpx, and CPU tick
 * interfaces. Only adds the DragonFly-specific {@link #lwp_gettid()} thread ID function.
 */
public interface DragonFlyBsdLibc extends FreeBsdLibc {
    DragonFlyBsdLibc INSTANCE = BsdLibcLoader.loadLibc(DragonFlyBsdLibc.class);

    /**
     * Returns the light weight process thread identifier for the calling thread.
     *
     * @return The LWP thread ID. Returns -1 on error.
     */
    int lwp_gettid();
}
