/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.freebsd.BsdSysctlUtilFFM;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdGlobalMemory;

/**
 * FFM-backed FreeBSD global memory. Native reads use {@link BsdSysctlUtilFFM}; shared parsing and the public API live
 * on {@link FreeBsdGlobalMemory}.
 */
@ThreadSafe
public class FreeBsdGlobalMemoryFFM extends FreeBsdGlobalMemory {

    @Override
    protected long queryVmStats() {
        // cached removed in FreeBSD 12 but was always set to 0
        int inactive = BsdSysctlUtilFFM.sysctl("vm.stats.vm.v_inactive_count", 0);
        int free = BsdSysctlUtilFFM.sysctl("vm.stats.vm.v_free_count", 0);
        return (inactive + free) * getPageSize();
    }

    @Override
    protected long queryPhysMem() {
        return BsdSysctlUtilFFM.sysctl("hw.physmem", 0L);
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new FreeBsdVirtualMemoryFFM(this);
    }
}
