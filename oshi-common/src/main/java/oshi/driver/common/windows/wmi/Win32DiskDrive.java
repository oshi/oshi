/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_DiskDrive}.
 */
@ThreadSafe
public class Win32DiskDrive {

    /**
     * The WMI class name.
     */
    public static final String WIN32_DISK_DRIVE = "Win32_DiskDrive";

    /**
     * Disk drive properties.
     */
    public enum DiskDriveProperty {
        /** INDEX property. */
        INDEX,
        /** MANUFACTURER property. */
        MANUFACTURER,
        /** MEDIATYPE property. */
        MEDIATYPE,
        /** MODEL property. */
        MODEL,
        /** NAME property. */
        NAME,
        /** SERIALNUMBER property. */
        SERIALNUMBER,
        /** SIZE property. */
        SIZE;
    }

    /**
     * Constructor.
     */
    protected Win32DiskDrive() {
    }

    /**
     * Queries disk drive information.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Information regarding each disk drive.
     */
    public static WmiResult<DiskDriveProperty> queryDiskDrive(WmiQueryExecutor h) {
        WmiQuery<DiskDriveProperty> diskDriveQuery = new WmiQuery<>(WIN32_DISK_DRIVE, DiskDriveProperty.class);
        return h.queryWMI(diskDriveQuery, false);
    }
}
