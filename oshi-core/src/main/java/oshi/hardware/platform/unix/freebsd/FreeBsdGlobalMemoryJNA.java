/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdGlobalMemory;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * JNA-backed FreeBSD global memory. Native reads use {@link BsdSysctlUtil}; shared parsing and the public API live on
 * {@link FreeBsdGlobalMemory}.
 */
@ThreadSafe
public class FreeBsdGlobalMemoryJNA extends FreeBsdGlobalMemory {

    @Override
    protected long queryVmStats() {
        // cached removed in FreeBSD 12 but was always set to 0.
        // Counters are uint32 from the kernel; widen to long before adding so the sum and the page-size multiply
        // don't truncate or sign-extend if either counter exceeds 2^31.
        long inactive = Integer.toUnsignedLong(BsdSysctlUtil.sysctl("vm.stats.vm.v_inactive_count", 0));
        long free = Integer.toUnsignedLong(BsdSysctlUtil.sysctl("vm.stats.vm.v_free_count", 0));
        return (inactive + free) * getPageSize();
    }

    @Override
    protected long queryPhysMem() {
        return BsdSysctlUtil.sysctl("hw.physmem", 0L);
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new FreeBsdVirtualMemoryJNA(this);
    }
}
