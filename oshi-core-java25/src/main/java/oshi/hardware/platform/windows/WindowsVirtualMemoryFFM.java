/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.driver.windows.perfmon.MemoryInformationFFM;
import oshi.driver.windows.perfmon.PagingFileFFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.PsapiFFM;
import oshi.hardware.common.AbstractVirtualMemory;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Virtual memory obtained from Performance Info using FFM.
 */
@ThreadSafe
final class WindowsVirtualMemoryFFM extends AbstractVirtualMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsVirtualMemoryFFM.class);

    private final WindowsGlobalMemoryFFM global;

    private final Supplier<Long> used = memoize(WindowsVirtualMemoryFFM::querySwapUsed, defaultExpiration());

    private final Supplier<Triplet<Long, Long, Long>> totalVmaxVused = memoize(
            WindowsVirtualMemoryFFM::querySwapTotalVirtMaxVirtUsed, defaultExpiration());

    private final Supplier<Pair<Long, Long>> swapInOut = memoize(WindowsVirtualMemoryFFM::queryPageSwaps,
            defaultExpiration());

    WindowsVirtualMemoryFFM(WindowsGlobalMemoryFFM windowsGlobalMemory) {
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
        return PagingFileFFM.querySwapUsed().getOrDefault(PagingPercentProperty.PERCENTUSAGE, 0L);
    }

    private static Triplet<Long, Long, Long> querySwapTotalVirtMaxVirtUsed() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment perfInfo = arena.allocate(PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT);
            int size = (int) PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT.byteSize();
            if (!PsapiFFM.GetPerformanceInfo(perfInfo, size)) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32FFM.GetLastError().orElse(-1));
                return new Triplet<>(0L, 0L, 0L);
            }
            long commitLimit = perfInfo.get(ValueLayout.JAVA_LONG, PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("CommitLimit")));
            long commitTotal = perfInfo.get(ValueLayout.JAVA_LONG, PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("CommitTotal")));
            long physTotal = perfInfo.get(ValueLayout.JAVA_LONG, PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("PhysicalTotal")));
            return new Triplet<>(commitLimit - physTotal, commitLimit, commitTotal);
        } catch (Throwable t) {
            LOG.error("Failed to get Performance Info: {}", t.getMessage());
            return new Triplet<>(0L, 0L, 0L);
        }
    }

    private static Pair<Long, Long> queryPageSwaps() {
        Map<PageSwapProperty, Long> valueMap = MemoryInformationFFM.queryPageSwaps();
        return new Pair<>(valueMap.getOrDefault(PageSwapProperty.PAGESINPUTPERSEC, 0L),
                valueMap.getOrDefault(PageSwapProperty.PAGESOUTPUTPERSEC, 0L));
    }
}
