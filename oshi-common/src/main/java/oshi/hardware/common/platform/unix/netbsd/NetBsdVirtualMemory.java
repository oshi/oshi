/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.tuples.Quartet;

/**
 * Memory info on NetBSD
 */
@ThreadSafe
final class NetBsdVirtualMemory extends AbstractVirtualMemory {

    private final NetBsdGlobalMemory global;

    private final Supplier<Quartet<Integer, Integer, Integer, Integer>> swapInfo = memoize(
            NetBsdVirtualMemory::queryVmstat, defaultExpiration());

    NetBsdVirtualMemory(NetBsdGlobalMemory netBsdGlobalMemory) {
        this.global = netBsdGlobalMemory;
    }

    @Override
    public long getSwapUsed() {
        return swapInfo.get().getA() * global.getPageSize();
    }

    @Override
    public long getSwapTotal() {
        return swapInfo.get().getB() * global.getPageSize();
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
        return swapInfo.get().getC();
    }

    @Override
    public long getSwapPagesOut() {
        return swapInfo.get().getD();
    }

    private static Quartet<Integer, Integer, Integer, Integer> queryVmstat() {
        int used = 0;
        int total = 0;
        int swapIn = 0;
        int swapOut = 0;
        for (String line : ExecutingCommand.runNative("vmstat -s")) {
            if (line.contains("swap pages in use")) {
                used = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("swap pages")) {
                total = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("pagein")) {
                swapIn = ParseUtil.getFirstIntValue(line);
            } else if (line.contains("pageout")) {
                swapOut = ParseUtil.getFirstIntValue(line);
            }
        }
        return new Quartet<>(used, total, swapIn, swapOut);
    }
}
