/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_DiskDriveToDiskPartition} using FFM.
 */
@ThreadSafe
public final class Win32DiskDriveToDiskPartitionFFM extends Win32DiskDriveToDiskPartition {

    private Win32DiskDriveToDiskPartitionFFM() {
    }

    /**
     * Queries the association between disk drive and partition.
     *
     * @param h An instantiated {@link WmiQueryHandlerFFM}. User should have already initialized COM.
     * @return Antecedent-dependent pairs of disk and partition.
     */
    public static WmiResult<DriveToPartitionProperty> queryDriveToPartition(WmiQueryHandlerFFM h) {
        WmiQuery<DriveToPartitionProperty> driveToPartitionQuery = new WmiQuery<>(WIN32_DISK_DRIVE_TO_DISK_PARTITION,
                DriveToPartitionProperty.class);
        return h.queryWMI(driveToPartitionQuery, false);
    }
}
