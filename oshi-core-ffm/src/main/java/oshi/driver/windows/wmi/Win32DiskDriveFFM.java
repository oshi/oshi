/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskDrive;
import oshi.driver.common.windows.wmi.Win32DiskDrive.DiskDriveProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class Win32DiskDriveFFM extends Win32DiskDrive {
    private Win32DiskDriveFFM() {
    }

    public static WmiResult<DiskDriveProperty> queryDiskDrive(WmiQueryExecutor h) {
        return Win32DiskDrive.queryDiskDrive(h);
    }
}
