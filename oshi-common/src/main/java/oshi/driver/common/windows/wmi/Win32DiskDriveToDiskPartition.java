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
        /** ANTECEDENT property. */
        ANTECEDENT,
        /** DEPENDENT property. */
        DEPENDENT;
    }

    /**
     * Constructor.
     */
    protected Win32DiskDriveToDiskPartition() {
    }

    /**
     * Queries disk drive to partition mapping.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Drive to partition mapping.
     */
    public static WmiResult<DriveToPartitionProperty> queryDriveToPartition(WmiQueryExecutor h) {
        WmiQuery<DriveToPartitionProperty> query = new WmiQuery<>(WIN32_DISK_DRIVE_TO_DISK_PARTITION,
                DriveToPartitionProperty.class);
        return h.queryWMI(query, false);
    }
}
