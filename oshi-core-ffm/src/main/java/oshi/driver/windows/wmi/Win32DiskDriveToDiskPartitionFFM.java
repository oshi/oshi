/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition;
import oshi.driver.common.windows.wmi.Win32DiskDriveToDiskPartition.DriveToPartitionProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class Win32DiskDriveToDiskPartitionFFM extends Win32DiskDriveToDiskPartition {
    private Win32DiskDriveToDiskPartitionFFM() {
    }

    public static WmiResult<DriveToPartitionProperty> queryDriveToPartition(WmiQueryExecutor h) {
        return Win32DiskDriveToDiskPartition.queryDriveToPartition(h);
    }
}
