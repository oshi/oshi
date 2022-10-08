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
 * Utility to query WMI class {@code Win32_DiskDrive}
 */
@ThreadSafe
public final class Win32DiskDrive {

    private static final String WIN32_DISK_DRIVE = "Win32_DiskDrive";

    /**
     * Disk drive properties
     */
    public enum DiskDriveProperty {
        INDEX, MANUFACTURER, MODEL, NAME, SERIALNUMBER, SIZE;
    }

    private Win32DiskDrive() {
    }

    /**
     * Queries the disk drive name info
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return Information regarding each disk drive.
     */
    public static WmiResult<DiskDriveProperty> queryDiskDrive(WmiQueryHandler h) {
        WmiQuery<DiskDriveProperty> diskDriveQuery = new WmiQuery<>(WIN32_DISK_DRIVE, DiskDriveProperty.class);
        return h.queryWMI(diskDriveQuery, false);
    }
}
