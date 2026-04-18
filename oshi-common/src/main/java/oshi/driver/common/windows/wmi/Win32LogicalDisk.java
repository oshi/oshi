/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_LogicalDisk}.
 */
@ThreadSafe
public class Win32LogicalDisk {

    /**
     * The WMI class name.
     */
    public static final String WIN32_LOGICAL_DISK = "Win32_LogicalDisk";

    /**
     * Logical disk properties.
     */
    public enum LogicalDiskProperty {
        ACCESS, DESCRIPTION, DRIVETYPE, FILESYSTEM, FREESPACE, NAME, PROVIDERNAME, SIZE, VOLUMENAME;
    }

    protected Win32LogicalDisk() {
    }

    /**
     * Builds the WMI class name with optional WHERE clause for logical disk queries.
     *
     * @param nameToMatch an optional string to filter match, null otherwise
     * @param localOnly   whether to only search local drives
     * @return the WMI class name with WHERE clause appended
     */
    public static String buildWmiClassNameWithWhere(String nameToMatch, boolean localOnly) {
        StringBuilder wmiClassName = new StringBuilder(WIN32_LOGICAL_DISK);
        boolean where = false;
        if (localOnly) {
            wmiClassName.append(" WHERE (DriveType = 2 OR DriveType = 3 OR DriveType = 6)");
            where = true;
        }
        if (nameToMatch != null) {
            wmiClassName.append(where ? " AND" : " WHERE").append(" Name=\"").append(nameToMatch.replace("\"", "\\\""))
                    .append('"');
        }
        return wmiClassName.toString();
    }
}
