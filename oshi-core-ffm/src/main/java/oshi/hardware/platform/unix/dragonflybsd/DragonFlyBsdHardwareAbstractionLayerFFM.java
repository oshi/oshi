/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.dragonflybsd;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.common.platform.unix.dragonflybsd.DragonFlyBsdHWDiskStore;
import oshi.hardware.platform.unix.freebsd.FreeBsdHardwareAbstractionLayerFFM;

/**
 * DragonFly BSD hardware abstraction layer. Every component behaves as FreeBSD's does except two, so this inherits the
 * FreeBSD wiring and replaces those: the processor, which reads its tick counters from {@code kern.cputime} rather than
 * {@code kern.cp_time}, and the disks, whose statistics come from {@code kern.devstat.all} because DragonFly has no
 * {@code geom} and no {@code iostat -x}.
 */
@ThreadSafe
public final class DragonFlyBsdHardwareAbstractionLayerFFM extends FreeBsdHardwareAbstractionLayerFFM {

    @Override
    public CentralProcessor createProcessor() {
        return new DragonFlyBsdCentralProcessorFFM();
    }

    @Override
    public List<HWDiskStore> getDiskStores() {
        return DragonFlyBsdHWDiskStore.getDisks(BsdSysctlUtilFFM.sysctl("kern.disks", ""),
                DragonFlyBsdHardwareAbstractionLayerFFM::queryDevstat);
    }

    private static byte[] queryDevstat() {
        MemorySegment m = BsdSysctlUtilFFM.sysctl(DragonFlyBsdHWDiskStore.DEVSTAT_SYSCTL);
        return m == null ? new byte[0] : m.toArray(ValueLayout.JAVA_BYTE);
    }
}
