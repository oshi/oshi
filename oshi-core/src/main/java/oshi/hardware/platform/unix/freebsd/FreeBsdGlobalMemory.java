/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Memory obtained by sysctl vm.stats
 */
@ThreadSafe
final class FreeBsdGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(this::queryVmStats, defaultExpiration());

    private final Supplier<Long> total = memoize(FreeBsdGlobalMemory::queryPhysMem);

    private final Supplier<Long> pageSize = memoize(FreeBsdGlobalMemory::queryPageSize);

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return available.get();
    }

    @Override
    public long getTotal() {
        return total.get();
    }

    @Override
    public long getPageSize() {
        return pageSize.get();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    private long queryVmStats() {
        // cached removed in FreeBSD 12 but was always set to 0
        int inactive = BsdSysctlUtil.sysctl("vm.stats.vm.v_inactive_count", 0);
        int free = BsdSysctlUtil.sysctl("vm.stats.vm.v_free_count", 0);
        return (inactive + free) * getPageSize();
    }

    private static long queryPhysMem() {
        return BsdSysctlUtil.sysctl("hw.physmem", 0L);
    }

    private static long queryPageSize() {
        // sysctl hw.pagesize doesn't work on FreeBSD 13
        return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("sysconf PAGESIZE"), 4096L);
    }

    private VirtualMemory createVirtualMemory() {
        return new FreeBsdVirtualMemory(this);
    }
}
