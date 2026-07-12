/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.common.windows.wmi.WmiUtil;
import oshi.hardware.PhysicalMemory;
import oshi.hardware.VirtualMemory;
import oshi.hardware.common.AbstractGlobalMemory;
import oshi.util.tuples.Triplet;

/**
 * Common Windows global memory logic shared between JNA and FFM implementations. Subclasses supply the native queries
 * (performance info, WMI physical-memory banks, virtual memory) via the {@code protected abstract} hooks; the
 * memoization and result assembly live here.
 */
@ThreadSafe
public abstract class WindowsGlobalMemory extends AbstractGlobalMemory {

    /** Default constructor. */
    protected WindowsGlobalMemory() {
    }

    private final Supplier<Triplet<Long, Long, Long>> availTotalSize = memoize(this::readPerfInfo, defaultExpiration());

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

    @Override
    public List<PhysicalMemory> getPhysicalMemory() {
        List<PhysicalMemory> physicalMemoryList = new ArrayList<>();
        if (isWindows10OrGreater()) {
            WmiResult<PhysicalMemoryProperty> bankMap = queryPhysicalMemory();
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
            WmiResult<PhysicalMemoryPropertyWin8> bankMap = queryPhysicalMemoryWin8();
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

    /**
     * Queries performance info for the available, total, and page-size values.
     *
     * @return a triplet of (available bytes, total bytes, page size)
     */
    protected abstract Triplet<Long, Long, Long> readPerfInfo();

    /**
     * Creates the backend-specific virtual memory instance.
     *
     * @return the virtual memory
     */
    protected abstract VirtualMemory createVirtualMemory();

    /**
     * @return {@code true} if running on Windows 10 or greater
     */
    protected abstract boolean isWindows10OrGreater();

    /**
     * Queries the Win32_PhysicalMemory banks (Windows 10+ schema).
     *
     * @return the WMI result
     */
    protected abstract WmiResult<PhysicalMemoryProperty> queryPhysicalMemory();

    /**
     * Queries the Win32_PhysicalMemory banks (pre-Windows 10 schema).
     *
     * @return the WMI result
     */
    protected abstract WmiResult<PhysicalMemoryPropertyWin8> queryPhysicalMemoryWin8();

    /**
     * memoryType.
     *
     * @param type the type
     * @return the result
     */

    protected static String memoryType(int type) {
        switch (type) {
            case 0:
                return "Unknown";
            case 1:
                return "Other";
            case 2:
                return "DRAM";
            case 3:
                return "Synchronous DRAM";
            case 4:
                return "Cache DRAM";
            case 5:
                return "EDO";
            case 6:
                return "EDRAM";
            case 7:
                return "VRAM";
            case 8:
                return "SRAM";
            case 9:
                return "RAM";
            case 10:
                return "ROM";
            case 11:
                return "Flash";
            case 12:
                return "EEPROM";
            case 13:
                return "FEPROM";
            case 14:
                return "EPROM";
            case 15:
                return "CDRAM";
            case 16:
                return "3DRAM";
            case 17:
                return "SDRAM";
            case 18:
                return "SGRAM";
            case 19:
                return "RDRAM";
            case 20:
                return "DDR";
            case 21:
                return "DDR2";
            case 22:
                return "BRAM";
            case 23:
                return "DDR FB-DIMM";
            default:
                return smBiosMemoryType(type);
        }
    }

    /**
     * smBiosMemoryType.
     *
     * @param type the type
     * @return the result
     */

    protected static String smBiosMemoryType(int type) {
        switch (type) {
            case 0x01:
                return "Other";
            case 0x03:
                return "DRAM";
            case 0x04:
                return "EDRAM";
            case 0x05:
                return "VRAM";
            case 0x06:
                return "SRAM";
            case 0x07:
                return "RAM";
            case 0x08:
                return "ROM";
            case 0x09:
                return "FLASH";
            case 0x0A:
                return "EEPROM";
            case 0x0B:
                return "FEPROM";
            case 0x0C:
                return "EPROM";
            case 0x0D:
                return "CDRAM";
            case 0x0E:
                return "3DRAM";
            case 0x0F:
                return "SDRAM";
            case 0x10:
                return "SGRAM";
            case 0x11:
                return "RDRAM";
            case 0x12:
                return "DDR";
            case 0x13:
                return "DDR2";
            case 0x14:
                return "DDR2 FB-DIMM";
            case 0x18:
                return "DDR3";
            case 0x19:
                return "FBD2";
            case 0x1A:
                return "DDR4";
            case 0x1B:
                return "LPDDR";
            case 0x1C:
                return "LPDDR2";
            case 0x1D:
                return "LPDDR3";
            case 0x1E:
                return "LPDDR4";
            case 0x1F:
                return "Logical non-volatile device";
            case 0x20:
                return "HBM";
            case 0x21:
                return "HBM2";
            case 0x22:
                return "DDR5";
            case 0x23:
                return "LPDDR5";
            case 0x24:
                return "HBM3";
            case 0x02:
            default:
                return "Unknown";
        }
    }
}
