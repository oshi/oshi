/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

public abstract class FreeBsdVirtualMemory extends AbstractVirtualMemory {

    private final FreeBsdGlobalMemory global;

    private final Supplier<Long> used = memoize(FreeBsdVirtualMemory::querySwapUsed, defaultExpiration());
    private final Supplier<Long> total = memoize(this::querySwapTotal, defaultExpiration());
    private final Supplier<Long> pagesIn = memoize(this::queryPagesIn, defaultExpiration());
    private final Supplier<Long> pagesOut = memoize(this::queryPagesOut, defaultExpiration());

    protected FreeBsdVirtualMemory(FreeBsdGlobalMemory global) {
        this.global = global;
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

    /** Reads {@code vm.swap_total} via the subclass's sysctl mechanism. */
    protected abstract long querySwapTotal();

    /** Reads {@code vm.stats.vm.v_swappgsin} via the subclass's sysctl mechanism. */
    protected abstract long queryPagesIn();

    /** Reads {@code vm.stats.vm.v_swappgsout} via the subclass's sysctl mechanism. */
    protected abstract long queryPagesOut();

    // Pure command-line: swapinfo -k. Used bytes is column index 2, KB → bytes via << 10.
    private static long querySwapUsed() {
        return parseSwapUsed(ExecutingCommand.getAnswerAt("swapinfo -k", 1));
    }

    /**
     * Extracts the "Used" column (index 2) from a single {@code swapinfo -k} data row and converts the KB value to
     * bytes. Returns 0 when the line is too short to be valid (fewer than 5 whitespace-separated columns).
     */
    static long parseSwapUsed(String swapInfoRow) {
        String[] split = ParseUtil.whitespaces.split(swapInfoRow);
        if (split.length < 5) {
            return 0L;
        }
        return ParseUtil.parseLongOrDefault(split[2], 0L) << 10;
    }
}
