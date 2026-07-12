/*
 * Copyright 2019-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.MemoryInformation.PageSwapProperty;
import oshi.driver.common.windows.perfmon.PagingFile.PagingPercentProperty;
import oshi.driver.windows.perfmon.MemoryInformationJNA;
import oshi.driver.windows.perfmon.PagingFileJNA;
import oshi.hardware.common.platform.windows.WindowsVirtualMemory;
import oshi.jna.Struct.CloseablePerformanceInformation;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Memory obtained from WMI
 */
@ThreadSafe
final class WindowsVirtualMemoryJNA extends WindowsVirtualMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsVirtualMemoryJNA.class);

    /**
     * Constructor for WindowsVirtualMemoryJNA.
     *
     * @param windowsGlobalMemory The parent global memory class instantiating this
     */
    WindowsVirtualMemoryJNA(WindowsGlobalMemoryJNA windowsGlobalMemory) {
        super(windowsGlobalMemory);
    }

    @Override
    protected long querySwapUsed() {
        return PagingFileJNA.querySwapUsed().getOrDefault(PagingPercentProperty.PERCENTUSAGE, 0L);
    }

    @Override
    protected Triplet<Long, Long, Long> querySwapTotalVirtMaxVirtUsed() {
        try (CloseablePerformanceInformation perfInfo = new CloseablePerformanceInformation()) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(perfInfo, perfInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return new Triplet<>(0L, 0L, 0L);
            }
            return new Triplet<>(perfInfo.CommitLimit.longValue() - perfInfo.PhysicalTotal.longValue(),
                    perfInfo.CommitLimit.longValue(), perfInfo.CommitTotal.longValue());
        }
    }

    @Override
    protected Pair<Long, Long> queryPageSwaps() {
        Map<PageSwapProperty, Long> valueMap = MemoryInformationJNA.queryPageSwaps();
        return new Pair<>(valueMap.getOrDefault(PageSwapProperty.PAGESINPUTPERSEC, 0L),
                valueMap.getOrDefault(PageSwapProperty.PAGESOUTPUTPERSEC, 0L));
    }
}
