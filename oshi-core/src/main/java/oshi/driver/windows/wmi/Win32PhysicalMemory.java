/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_PhysicalMemory}
 */
@ThreadSafe
public final class Win32PhysicalMemory {

    private static final String WIN32_PHYSICAL_MEMORY = "Win32_PhysicalMemory";

    /**
     * Physical Memory properties for Win10 and later.
     */
    public enum PhysicalMemoryProperty {
        BANKLABEL, CAPACITY, SPEED, MANUFACTURER, SMBIOSMEMORYTYPE
    }

    /**
     * Physical Memory properties for Win8 and earlier.
     */
    public enum PhysicalMemoryPropertyWin8 {
        BANKLABEL, CAPACITY, SPEED, MANUFACTURER, MEMORYTYPE
    }

    private Win32PhysicalMemory() {
    }

    /**
     * Queries physical memory info for Win10 and later.
     *
     * @return Information regarding physical memory.
     */
    public static WmiResult<PhysicalMemoryProperty> queryphysicalMemory() {
        WmiQuery<PhysicalMemoryProperty> physicalMemoryQuery = new WmiQuery<>(WIN32_PHYSICAL_MEMORY,
                PhysicalMemoryProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(physicalMemoryQuery);
    }

    /**
     * Queries physical memory info for Win8 and earlier.
     *
     * @return Information regarding physical memory.
     */
    public static WmiResult<PhysicalMemoryPropertyWin8> queryphysicalMemoryWin8() {
        WmiQuery<PhysicalMemoryPropertyWin8> physicalMemoryQuery = new WmiQuery<>(WIN32_PHYSICAL_MEMORY,
                PhysicalMemoryPropertyWin8.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(physicalMemoryQuery);
    }
}
