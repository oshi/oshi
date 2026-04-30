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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.windows.wmi.Win32PhysicalMemoryFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.ffm.windows.Kernel32FFM;
import oshi.ffm.windows.PsapiFFM;
import oshi.ffm.windows.VersionHelpersFFM;
import oshi.hardware.PhysicalMemory;
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

    private final Supplier<Triplet<Long, Long, Long>> availTotalSize = memoize(WindowsGlobalMemoryFFM::readPerfInfo,
            defaultExpiration());

    private final Supplier<VirtualMemory> vm = memoize(this::createVirtualMemory);

    @Override
    public long getAvailable() {
        return availTotalSize.get().getA();
    }

    @Override
    public long getTotal() {
        return availTotalSize.get().getB();
    }

    @Override
    public long getPageSize() {
        return availTotalSize.get().getC();
    }

    @Override
    public VirtualMemory getVirtualMemory() {
        return vm.get();
    }

    private VirtualMemory createVirtualMemory() {
        return new WindowsVirtualMemoryFFM(this);
    }

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        List<PhysicalMemory> physicalMemoryList = new ArrayList<>();
        if (IS_WINDOWS10_OR_GREATER) {
            WmiResult<PhysicalMemoryProperty> bankMap = Win32PhysicalMemoryFFM.queryPhysicalMemory();
            for (int index = 0; index < bankMap.getResultCount(); index++) {
                String bankLabel = WmiUtilFFM.getString(bankMap, PhysicalMemoryProperty.BANKLABEL, index);
                long capacity = WmiUtilFFM.getUint64(bankMap, PhysicalMemoryProperty.CAPACITY, index);
                long speed = WmiUtilFFM.getUint32(bankMap, PhysicalMemoryProperty.SPEED, index) * 1_000_000L;
                String manufacturer = WmiUtilFFM.getString(bankMap, PhysicalMemoryProperty.MANUFACTURER, index);
                String memType = smBiosMemoryType(
                        WmiUtilFFM.getUint32(bankMap, PhysicalMemoryProperty.SMBIOSMEMORYTYPE, index));
                String partNumber = WmiUtilFFM.getString(bankMap, PhysicalMemoryProperty.PARTNUMBER, index);
                String serialNumber = WmiUtilFFM.getString(bankMap, PhysicalMemoryProperty.SERIALNUMBER, index);
                physicalMemoryList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memType, partNumber,
                        serialNumber));
            }
        } else {
            WmiResult<PhysicalMemoryPropertyWin8> bankMap = Win32PhysicalMemoryFFM.queryPhysicalMemoryWin8();
            for (int index = 0; index < bankMap.getResultCount(); index++) {
                String bankLabel = WmiUtilFFM.getString(bankMap, PhysicalMemoryPropertyWin8.BANKLABEL, index);
                long capacity = WmiUtilFFM.getUint64(bankMap, PhysicalMemoryPropertyWin8.CAPACITY, index);
                long speed = WmiUtilFFM.getUint32(bankMap, PhysicalMemoryPropertyWin8.SPEED, index) * 1_000_000L;
                String manufacturer = WmiUtilFFM.getString(bankMap, PhysicalMemoryPropertyWin8.MANUFACTURER, index);
                String memType = memoryType(
                        WmiUtilFFM.getUint16(bankMap, PhysicalMemoryPropertyWin8.MEMORYTYPE, index));
                String partNumber = WmiUtilFFM.getString(bankMap, PhysicalMemoryPropertyWin8.PARTNUMBER, index);
                String serialNumber = WmiUtilFFM.getString(bankMap, PhysicalMemoryPropertyWin8.SERIALNUMBER, index);
                physicalMemoryList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memType, partNumber,
                        serialNumber));
            }
        }
        return physicalMemoryList;
    }

    private static Triplet<Long, Long, Long> readPerfInfo() {
        try (Arena arena = Arena.ofConfined()) {
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
        } catch (Throwable t) {
            LOG.error("Failed to get Performance Info: {}", t.getMessage());
            return new Triplet<>(0L, 0L, 4096L);
        }
    }
}
