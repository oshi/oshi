/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdGlobalMemory;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdVirtualMemory;

/**
 * FFM-backed FreeBSD virtual memory. Native sysctl reads use {@link BsdSysctlUtilFFM}; the public API and the swapinfo
 * command-line parsing live on {@link FreeBsdVirtualMemory}.
 */
@ThreadSafe
final class FreeBsdVirtualMemoryFFM extends FreeBsdVirtualMemory {

    FreeBsdVirtualMemoryFFM(FreeBsdGlobalMemory global) {
        super(global);
    }

    @Override
    protected long querySwapTotal() {
        return BsdSysctlUtilFFM.sysctl("vm.swap_total", 0L);
    }

    @Override
    protected long queryPagesIn() {
        return BsdSysctlUtilFFM.sysctl("vm.stats.vm.v_swappgsin", 0L);
    }

    @Override
    protected long queryPagesOut() {
        return BsdSysctlUtilFFM.sysctl("vm.stats.vm.v_swappgsout", 0L);
    }
}
