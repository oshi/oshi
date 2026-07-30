/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayerJNA;

/**
 * DragonFly BSD hardware abstraction layer. Every component behaves as FreeBSD's does except the processor, which reads
 * its tick counters from {@code kern.cputime} rather than {@code kern.cp_time}, so this inherits the FreeBSD wiring and
 * replaces only that one.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayerJNA extends FreeBsdHardwareAbstractionLayerJNA {

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessorJNA();
    }
}
