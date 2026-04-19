/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_DiskDriveToDiskPartition}.
 */
@ThreadSafe
public class Win32DiskDriveToDiskPartition {

    /**
     * The WMI class name.
     */
    public static final String WIN32_DISK_DRIVE_TO_DISK_PARTITION = "Win32_DiskDriveToDiskPartition";

    /**
     * Links disk drives to partitions.
     */
    public enum DriveToPartitionProperty {
        ANTECEDENT, DEPENDENT;
    }

    protected Win32DiskDriveToDiskPartition() {
    }
}
