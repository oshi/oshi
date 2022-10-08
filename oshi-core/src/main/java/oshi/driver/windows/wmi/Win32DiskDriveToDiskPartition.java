/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_DiskDriveToDiskPartition}
 */
@ThreadSafe
public final class Win32DiskDriveToDiskPartition {

    private static final String WIN32_DISK_DRIVE_TO_DISK_PARTITION = "Win32_DiskDriveToDiskPartition";

    /**
     * Links disk drives to partitions
     */
    public enum DriveToPartitionProperty {
        ANTECEDENT, DEPENDENT;
    }

    private Win32DiskDriveToDiskPartition() {
    }

    /**
     * Queries the association between disk drive and partition.
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return Antecedent-dependent pairs of disk and partition.
     */
    public static WmiResult<DriveToPartitionProperty> queryDriveToPartition(WmiQueryHandler h) {
        WmiQuery<DriveToPartitionProperty> driveToPartitionQuery = new WmiQuery<>(WIN32_DISK_DRIVE_TO_DISK_PARTITION,
                DriveToPartitionProperty.class);
        return h.queryWMI(driveToPartitionQuery, false);
    }
}
