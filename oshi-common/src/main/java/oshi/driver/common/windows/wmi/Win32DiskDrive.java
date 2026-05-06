/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_DiskDrive}.
 */
@ThreadSafe
public class Win32DiskDrive {

    /**
     * The WMI class name.
     */
    public static final String WIN32_DISK_DRIVE = "Win32_DiskDrive";

    /**
     * Disk drive properties.
     */
    public enum DiskDriveProperty {
        INDEX, MANUFACTURER, MEDIATYPE, MODEL, NAME, SERIALNUMBER, SIZE;
    }

    protected Win32DiskDrive() {
    }
}
