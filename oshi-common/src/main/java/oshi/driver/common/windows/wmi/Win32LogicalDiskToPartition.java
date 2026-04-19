/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_LogicalDiskToPartition}.
 */
@ThreadSafe
public class Win32LogicalDiskToPartition {

    /**
     * The WMI class name.
     */
    public static final String WIN32_LOGICAL_DISK_TO_PARTITION = "Win32_LogicalDiskToPartition";

    /**
     * Links disk drives to partitions.
     */
    public enum DiskToPartitionProperty {
        ANTECEDENT, DEPENDENT, ENDINGADDRESS, STARTINGADDRESS;
    }

    protected Win32LogicalDiskToPartition() {
    }
}
