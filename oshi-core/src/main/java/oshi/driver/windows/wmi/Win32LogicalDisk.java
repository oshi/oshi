/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_LogicalDisk}
 */
@ThreadSafe
public final class Win32LogicalDisk {

    private static final String WIN32_LOGICAL_DISK = "Win32_LogicalDisk";

    /**
     * Logical disk properties.
     */
    public enum LogicalDiskProperty {
        ACCESS, DESCRIPTION, DRIVETYPE, FILESYSTEM, FREESPACE, NAME, PROVIDERNAME, SIZE, VOLUMENAME;
    }

    private Win32LogicalDisk() {
    }

    /**
     * Queries logical disk information
     *
     * @param nameToMatch an optional string to filter match, null otherwise
     * @param localOnly   Whether to only search local drives
     * @return Logical Disk Information
     */
    public static WmiResult<LogicalDiskProperty> queryLogicalDisk(String nameToMatch, boolean localOnly) {
        StringBuilder wmiClassName = new StringBuilder(WIN32_LOGICAL_DISK);
        boolean where = false;
        if (localOnly) {
            wmiClassName.append(" WHERE DriveType != 4");
            where = true;
        }
        if (nameToMatch != null) {
            wmiClassName.append(where ? " AND" : " WHERE").append(" Name=\"").append(nameToMatch).append('\"');
        }
        WmiQuery<LogicalDiskProperty> logicalDiskQuery = new WmiQuery<>(wmiClassName.toString(),
                LogicalDiskProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(logicalDiskQuery);
    }
}
