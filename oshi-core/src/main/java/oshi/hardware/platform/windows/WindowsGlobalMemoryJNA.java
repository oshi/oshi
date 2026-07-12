/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.windows.wmi.Win32PhysicalMemoryJNA;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.windows.WindowsGlobalMemory;
import oshi.jna.Struct.CloseablePerformanceInformation;
import oshi.util.tuples.Triplet;

/**
 * Memory obtained by Performance Info.
 * <p>
 * Not {@code final} so that tests can subclass it to force the {@link #isWindows10OrGreater()} version gate and
 * exercise the pre-Windows-10 fallback query path against the real system.
 */
@ThreadSafe
class WindowsGlobalMemoryJNA extends WindowsGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemoryJNA.class);

    private static final boolean IS_WINDOWS10_OR_GREATER = VersionHelpers.IsWindows10OrGreater();

    @Override
    protected Triplet<Long, Long, Long> readPerfInfo() {
        try (CloseablePerformanceInformation performanceInfo = new CloseablePerformanceInformation()) {
            if (!Psapi.INSTANCE.GetPerformanceInfo(performanceInfo, performanceInfo.size())) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32.INSTANCE.GetLastError());
                return new Triplet<>(0L, 0L, 4096L);
            }
            long pageSize = performanceInfo.PageSize.longValue();
            long memAvailable = pageSize * performanceInfo.PhysicalAvailable.longValue();
            long memTotal = pageSize * performanceInfo.PhysicalTotal.longValue();
            return new Triplet<>(memAvailable, memTotal, pageSize);
        }
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new WindowsVirtualMemoryJNA(this);
    }

    @Override
    protected boolean isWindows10OrGreater() {
        return IS_WINDOWS10_OR_GREATER;
    }

    @Override
    protected WmiResult<PhysicalMemoryProperty> queryPhysicalMemory() {
        return Win32PhysicalMemoryJNA.queryPhysicalMemory();
    }

    @Override
    protected WmiResult<PhysicalMemoryPropertyWin8> queryPhysicalMemoryWin8() {
        return Win32PhysicalMemoryJNA.queryPhysicalMemoryWin8();
    }
}
