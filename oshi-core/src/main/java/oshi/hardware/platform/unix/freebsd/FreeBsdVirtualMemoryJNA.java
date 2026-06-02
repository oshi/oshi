/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdGlobalMemory;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdVirtualMemory;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD virtual memory. Native sysctl reads use {@link BsdSysctlUtil}; the public API and the swapinfo
 * command-line parsing live on {@link FreeBsdVirtualMemory}.
 */
@ThreadSafe
final class FreeBsdVirtualMemoryJNA extends FreeBsdVirtualMemory {

    FreeBsdVirtualMemoryJNA(FreeBsdGlobalMemory global) {
        super(global);
    }

    @Override
    protected long querySwapTotal() {
        return BsdSysctlUtil.sysctl("vm.swap_total", 0L);
    }

    @Override
    protected long queryPagesIn() {
        return BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsin", 0L);
    }

    @Override
    protected long queryPagesOut() {
        return BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsout", 0L);
    }
}
