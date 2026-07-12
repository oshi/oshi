/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static org.slf4j.event.Level.ERROR;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.windows.wmi.Win32PhysicalMemoryFFM;
import oshi.ffm.platform.windows.Kernel32FFM;
import oshi.ffm.platform.windows.PsapiFFM;
import oshi.ffm.platform.windows.VersionHelpersFFM;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.windows.WindowsGlobalMemory;
import oshi.util.tuples.Triplet;

/**
 * Memory obtained by Performance Info using FFM.
 */
@ThreadSafe
final class WindowsGlobalMemoryFFM extends WindowsGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemoryFFM.class);

    private static final boolean IS_WINDOWS10_OR_GREATER = VersionHelpersFFM.IsWindows10OrGreater();

    @Override
    protected Triplet<Long, Long, Long> readPerfInfo() {
        return callInArenaOrDefault(arena -> {
            MemorySegment perfInfo = arena.allocate(PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT);
            int size = (int) PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT.byteSize();
            if (!PsapiFFM.GetPerformanceInfo(perfInfo, size)) {
                LOG.error("Failed to get Performance Info. Error code: {}", Kernel32FFM.GetLastError().orElse(-1));
                return new Triplet<>(0L, 0L, 4096L);
            }
            long pageSize = perfInfo.get(ValueLayout.JAVA_LONG, PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("PageSize")));
            long physAvail = perfInfo.get(ValueLayout.JAVA_LONG, PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("PhysicalAvailable")));
            long physTotal = perfInfo.get(ValueLayout.JAVA_LONG, PsapiFFM.PERFORMANCE_INFORMATION_LAYOUT
                    .byteOffset(MemoryLayout.PathElement.groupElement("PhysicalTotal")));
            long memAvailable = pageSize * physAvail;
            long memTotal = pageSize * physTotal;
            return new Triplet<>(memAvailable, memTotal, pageSize);
        }, LOG, ERROR, "Failed to get Performance Info", new Triplet<>(0L, 0L, 4096L));
    }

    @Override
    protected VirtualMemory createVirtualMemory() {
        return new WindowsVirtualMemoryFFM(this);
    }

    @Override
    protected boolean isWindows10OrGreater() {
        return IS_WINDOWS10_OR_GREATER;
    }

    @Override
    protected WmiResult<PhysicalMemoryProperty> queryPhysicalMemory() {
        return Win32PhysicalMemoryFFM.queryPhysicalMemory();
    }

    @Override
    protected WmiResult<PhysicalMemoryPropertyWin8> queryPhysicalMemoryWin8() {
        return Win32PhysicalMemoryFFM.queryPhysicalMemoryWin8();
    }
}
