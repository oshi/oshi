/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.driver.windows.perfmon.MemoryInformationJNA;
import oshi.driver.windows.perfmon.PagingFileJNA;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.jna.Struct.CloseablePerformanceInformation;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Memory obtained from WMI
 */
@ThreadSafe
final class WindowsVirtualMemoryJNA extends AbstractVirtualMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsVirtualMemoryJNA.class);

    private final WindowsGlobalMemoryJNA global;

    private final Supplier<Long> used = memoize(WindowsVirtualMemoryJNA::querySwapUsed, defaultExpiration());

    private final Supplier<Triplet<Long, Long, Long>> totalVmaxVused = memoize(
            WindowsVirtualMemoryJNA::querySwapTotalVirtMaxVirtUsed, defaultExpiration());

    private final Supplier<Pair<Long, Long>> swapInOut = memoize(WindowsVirtualMemoryJNA::queryPageSwaps,
            defaultExpiration());

    /**
     * Constructor for WindowsVirtualMemoryJNA.
     *
     * @param windowsGlobalMemory The parent global memory class instantiating this
     */
    WindowsVirtualMemoryJNA(WindowsGlobalMemoryJNA windowsGlobalMemory) {
        this.global = windowsGlobalMemory;
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

    private static long querySwapUsed() {
        return PagingFileJNA.querySwapUsed().getOrDefault(PagingPercentProperty.PERCENTUSAGE, 0L);
    }

    private static Triplet<Long, Long, Long> querySwapTotalVirtMaxVirtUsed() {
        try (CloseablePerformanceInformation perfInfo = new CloseablePerformanceInformation()) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return new Triplet<>(0L, 0L, 0L);
            }
            return new Triplet<>(perfInfo.CommitLimit.longValue() - perfInfo.PhysicalTotal.longValue(),
                    perfInfo.CommitLimit.longValue(), perfInfo.CommitTotal.longValue());
        }
    }

    private static Pair<Long, Long> queryPageSwaps() {
        Map<PageSwapProperty, Long> valueMap = MemoryInformationJNA.queryPageSwaps();
        return new Pair<>(valueMap.getOrDefault(PageSwapProperty.PAGESINPUTPERSEC, 0L),
                valueMap.getOrDefault(PageSwapProperty.PAGESOUTPUTPERSEC, 0L));
    }
}
