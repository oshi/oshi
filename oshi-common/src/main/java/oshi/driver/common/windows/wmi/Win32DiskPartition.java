/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_DiskPartition}.
 */
@ThreadSafe
public class Win32DiskPartition {

    /**
     * The WMI class name.
     */
    public static final String WIN32_DISK_PARTITION = "Win32_DiskPartition";

    /**
     * Disk partition properties.
     */
    public enum DiskPartitionProperty {
        INDEX, DESCRIPTION, DEVICEID, DISKINDEX, NAME, SIZE, TYPE;
    }

    protected Win32DiskPartition() {
    }
}
