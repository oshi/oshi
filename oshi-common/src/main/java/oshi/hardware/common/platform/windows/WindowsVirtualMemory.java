/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Common Windows virtual memory logic shared between JNA and FFM implementations. Subclasses supply the native queries
 * (paging usage, performance info, page swaps) via the {@code protected abstract} hooks; the memoization and page-size
 * scaling live here.
 */
@ThreadSafe
public abstract class WindowsVirtualMemory extends AbstractVirtualMemory {

    private final WindowsGlobalMemory global;

    private final Supplier<Long> used = memoize(this::querySwapUsed, defaultExpiration());

    private final Supplier<Triplet<Long, Long, Long>> totalVmaxVused = memoize(this::querySwapTotalVirtMaxVirtUsed,
            defaultExpiration());

    private final Supplier<Pair<Long, Long>> swapInOut = memoize(this::queryPageSwaps, defaultExpiration());

    /**
     * Constructor.
     *
     * @param global the parent global memory instance, used to scale results by the page size
     */
    protected WindowsVirtualMemory(WindowsGlobalMemory global) {
        this.global = global;
    }

    @Override
    public long getSwapUsed() {
        return this.global.getPageSize() * used.get();
    }

    @Override
    public long getSwapTotal() {
        return this.global.getPageSize() * totalVmaxVused.get().getA();
    }

    @Override
    public long getVirtualMax() {
        return this.global.getPageSize() * totalVmaxVused.get().getB();
    }

    @Override
    public long getVirtualInUse() {
        return this.global.getPageSize() * totalVmaxVused.get().getC();
    }

    @Override
    public long getSwapPagesIn() {
        return swapInOut.get().getA();
    }

    @Override
    public long getSwapPagesOut() {
        return swapInOut.get().getB();
    }

    /**
     * Queries the swap (paging file) usage.
     *
     * @return the swap used, in pages
     */
    protected abstract long querySwapUsed();

    /**
     * Queries performance info for the swap total, virtual max, and virtual in-use values.
     *
     * @return a triplet of (swap total, virtual max, virtual in use), in pages
     */
    protected abstract Triplet<Long, Long, Long> querySwapTotalVirtMaxVirtUsed();

    /**
     * Queries the page swap-in and swap-out rates.
     *
     * @return a pair of (pages in, pages out)
     */
    protected abstract Pair<Long, Long> queryPageSwaps();
}
