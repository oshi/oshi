/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskDrive;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_DiskDrive} using FFM.
 */
@ThreadSafe
public final class Win32DiskDriveFFM extends Win32DiskDrive {

    private Win32DiskDriveFFM() {
    }

    /**
     * Queries disk drive information.
     *
     * @param h An instantiated {@link WmiQueryHandlerFFM}. User should have already initialized COM.
     * @return Information regarding each disk drive.
     */
    public static WmiResult<DiskDriveProperty> queryDiskDrive(WmiQueryHandlerFFM h) {
        WmiQuery<DiskDriveProperty> diskDriveQuery = new WmiQuery<>(WIN32_DISK_DRIVE, DiskDriveProperty.class);
        return h.queryWMI(diskDriveQuery, false);
    }
}
