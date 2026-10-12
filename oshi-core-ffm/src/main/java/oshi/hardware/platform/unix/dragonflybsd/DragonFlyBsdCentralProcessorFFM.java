/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.platform.unix.freebsd.FreeBsdCentralProcessorFFM;

/**
 * DragonFly BSD Central Processor (FFM). Inherits {@code kern.cp_time} system tick logic from
 * {@link FreeBsdCentralProcessorFFM} and reads per-CPU ticks from DragonFly's {@code kern.cputime} sysctl (FreeBSD uses
 * {@code kern.cp_times}).
 */
@ThreadSafe
public class DragonFlyBsdCentralProcessorFFM extends FreeBsdCentralProcessorFFM {

    @Override
    public long[][] queryProcessorCpuLoadTicks() {
        return queryKinfoCputimeTicks();
    }
}
