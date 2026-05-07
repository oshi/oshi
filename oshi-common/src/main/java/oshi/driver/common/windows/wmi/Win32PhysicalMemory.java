/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enums for WMI class {@code Win32_PhysicalMemory}.
 */
@ThreadSafe
public class Win32PhysicalMemory {

    /**
     * The WMI class name.
     */
    public static final String WIN32_PHYSICAL_MEMORY = "Win32_PhysicalMemory";

    /**
     * Physical Memory properties for Win10 and later.
     */
    public enum PhysicalMemoryProperty {
        /** BANKLABEL property. */
        BANKLABEL,
        /** CAPACITY property. */
        CAPACITY,
        /** SPEED property. */
        SPEED,
        /** MANUFACTURER property. */
        MANUFACTURER,
        /** PARTNUMBER property. */
        PARTNUMBER,
        /** SMBIOSMEMORYTYPE property. */
        SMBIOSMEMORYTYPE,
        /** SERIALNUMBER property. */
        SERIALNUMBER
    }

    /**
     * Physical Memory properties for Win8 and earlier.
     */
    public enum PhysicalMemoryPropertyWin8 {
        /** BANKLABEL property. */
        BANKLABEL,
        /** CAPACITY property. */
        CAPACITY,
        /** SPEED property. */
        SPEED,
        /** MANUFACTURER property. */
        MANUFACTURER,
        /** MEMORYTYPE property. */
        MEMORYTYPE,
        /** PARTNUMBER property. */
        PARTNUMBER,
        /** SERIALNUMBER property. */
        SERIALNUMBER
    }

    protected Win32PhysicalMemory() {
    }
}
