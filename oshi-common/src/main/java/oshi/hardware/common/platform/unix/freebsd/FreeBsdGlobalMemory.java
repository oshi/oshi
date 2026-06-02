/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public abstract class FreeBsdGlobalMemory extends AbstractGlobalMemory {

    private final Supplier<Long> available = memoize(this::queryVmStats, defaultExpiration());
    private final Supplier<Long> total = memoize(this::queryPhysMem);
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

    /**
     * Computes available memory from FreeBSD's vm.stats.vm.v_inactive_count + v_free_count, multiplied by page size.
     * Subclasses provide the sysctl read mechanism (JNA via BsdSysctlUtil, FFM via direct sysctlbyname binding).
     */
    protected abstract long queryVmStats();

    /** Reads {@code hw.physmem} via the subclass's sysctl mechanism. */
    protected abstract long queryPhysMem();

    /** Constructs the matching FreeBsdVirtualMemory concrete (JNA or FFM) for this instance. */
    protected abstract VirtualMemory createVirtualMemory();

    // sysctl hw.pagesize doesn't work on FreeBSD 13; fall back to sysconf(3).
    private static long queryPageSize() {
        return ParseUtil.parseLongOrDefault(ExecutingCommand.getFirstAnswer("sysconf PAGESIZE"), 4096L);
    }
}
