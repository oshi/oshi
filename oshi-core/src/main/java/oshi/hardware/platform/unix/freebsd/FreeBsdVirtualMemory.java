/*
 * Copyright 2019-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * Memory obtained by swapinfo
 */
@ThreadSafe
final class FreeBsdVirtualMemory extends AbstractVirtualMemory {

    private final FreeBsdGlobalMemory global;

    private final Supplier<Long> used = memoize(FreeBsdVirtualMemory::querySwapUsed, defaultExpiration());

    private final Supplier<Long> total = memoize(FreeBsdVirtualMemory::querySwapTotal, defaultExpiration());

    private final Supplier<Long> pagesIn = memoize(FreeBsdVirtualMemory::queryPagesIn, defaultExpiration());

    private final Supplier<Long> pagesOut = memoize(FreeBsdVirtualMemory::queryPagesOut, defaultExpiration());

    FreeBsdVirtualMemory(FreeBsdGlobalMemory freeBsdGlobalMemory) {
        this.global = freeBsdGlobalMemory;
    }

    @Override
    public long getSwapUsed() {
        return used.get();
    }

    @Override
    public long getSwapTotal() {
        return total.get();
    }

    @Override
    public long getVirtualMax() {
        return this.global.getTotal() + getSwapTotal();
    }

    @Override
    public long getVirtualInUse() {
        return this.global.getTotal() - this.global.getAvailable() + getSwapUsed();
    }

    @Override
    public long getSwapPagesIn() {
        return pagesIn.get();
    }

    @Override
    public long getSwapPagesOut() {
        return pagesOut.get();
    }

    private static long querySwapUsed() {
        String swapInfo = ExecutingCommand.getAnswerAt("swapinfo -k", 1);
        String[] split = ParseUtil.whitespaces.split(swapInfo);
        if (split.length < 5) {
            return 0L;
        }
        return ParseUtil.parseLongOrDefault(split[2], 0L) << 10;
    }

    private static long querySwapTotal() {
        return BsdSysctlUtil.sysctl("vm.swap_total", 0L);
    }

    private static long queryPagesIn() {
        return BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsin", 0L);
    }

    private static long queryPagesOut() {
        return BsdSysctlUtil.sysctl("vm.stats.vm.v_swappgsout", 0L);
    }
}
