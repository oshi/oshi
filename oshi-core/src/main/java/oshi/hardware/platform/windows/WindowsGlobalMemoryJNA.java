/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.windows.wmi.Win32PhysicalMemoryJNA;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.platform.windows.WindowsGlobalMemory;
import oshi.jna.Struct.CloseablePerformanceInformation;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Triplet;

/**
 * Memory obtained by Performance Info.
 */
@ThreadSafe
final class WindowsGlobalMemoryJNA extends WindowsGlobalMemory {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGlobalMemoryJNA.class);

    private static final boolean IS_WINDOWS10_OR_GREATER = VersionHelpers.IsWindows10OrGreater();

    private final Supplier<Triplet<Long, Long, Long>> availTotalSize = memoize(WindowsGlobalMemoryJNA::readPerfInfo,
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
        return new WindowsVirtualMemoryJNA(this);
    }

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        List<PhysicalMemory> physicalMemoryList = new ArrayList<>();
        if (IS_WINDOWS10_OR_GREATER) {
            WmiResult<PhysicalMemoryProperty> bankMap = Win32PhysicalMemoryJNA.queryPhysicalMemory();
            for (int index = 0; index < bankMap.getResultCount(); index++) {
                String bankLabel = WmiUtil.getString(bankMap, PhysicalMemoryProperty.BANKLABEL, index);
                long capacity = WmiUtil.getUint64(bankMap, PhysicalMemoryProperty.CAPACITY, index);
                long speed = WmiUtil.getUint32(bankMap, PhysicalMemoryProperty.SPEED, index) * 1_000_000L;
                String manufacturer = WmiUtil.getString(bankMap, PhysicalMemoryProperty.MANUFACTURER, index);
                String memoryType = smBiosMemoryType(
                        WmiUtil.getUint32(bankMap, PhysicalMemoryProperty.SMBIOSMEMORYTYPE, index));
                String partNumber = WmiUtil.getString(bankMap, PhysicalMemoryProperty.PARTNUMBER, index);
                String serialNumber = WmiUtil.getString(bankMap, PhysicalMemoryProperty.SERIALNUMBER, index);
                physicalMemoryList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType,
                        partNumber, serialNumber));
            }
        } else {
            WmiResult<PhysicalMemoryPropertyWin8> bankMap = Win32PhysicalMemoryJNA.queryPhysicalMemoryWin8();
            for (int index = 0; index < bankMap.getResultCount(); index++) {
                String bankLabel = WmiUtil.getString(bankMap, PhysicalMemoryPropertyWin8.BANKLABEL, index);
                long capacity = WmiUtil.getUint64(bankMap, PhysicalMemoryPropertyWin8.CAPACITY, index);
                long speed = WmiUtil.getUint32(bankMap, PhysicalMemoryPropertyWin8.SPEED, index) * 1_000_000L;
                String manufacturer = WmiUtil.getString(bankMap, PhysicalMemoryPropertyWin8.MANUFACTURER, index);
                String memoryType = memoryType(
                        WmiUtil.getUint16(bankMap, PhysicalMemoryPropertyWin8.MEMORYTYPE, index));
                String partNumber = WmiUtil.getString(bankMap, PhysicalMemoryPropertyWin8.PARTNUMBER, index);
                String serialNumber = WmiUtil.getString(bankMap, PhysicalMemoryPropertyWin8.SERIALNUMBER, index);
                physicalMemoryList.add(new PhysicalMemory(bankLabel, capacity, speed, manufacturer, memoryType,
                        partNumber, serialNumber));
            }
        }
        return physicalMemoryList;
    }

    private static Triplet<Long, Long, Long> readPerfInfo() {
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
}
