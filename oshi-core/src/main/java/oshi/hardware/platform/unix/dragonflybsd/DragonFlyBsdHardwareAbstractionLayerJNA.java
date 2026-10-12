/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import java.util.List;

import com.sun.jna.Memory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.dragonflybsd.DragonFlyBsdHWDiskStore;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayerJNA;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * DragonFly BSD hardware abstraction layer. Every component behaves as FreeBSD's does except two, so this inherits the
 * FreeBSD wiring and replaces those: the processor, which reads its tick counters from {@code kern.cputime} rather than
 * {@code kern.cp_time}, and the disks, whose statistics come from {@code kern.devstat.all} because DragonFly has no
 * {@code geom} and no {@code iostat -x}.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayerJNA extends FreeBsdHardwareAbstractionLayerJNA {

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessorJNA();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return DragonFlyBsdHWDiskStore.getDisks(BsdSysctlUtil.sysctl("kern.disks", ""),
                DragonFlyBsdHardwareAbstractionLayerJNA::queryDevstat);
    }

    private static byte[] queryDevstat() {
        try (Memory m = BsdSysctlUtil.sysctl(DragonFlyBsdHWDiskStore.DEVSTAT_SYSCTL)) {
            return m == null ? new byte[0] : m.getByteArray(0, (int) m.size());
        }
    }
}
