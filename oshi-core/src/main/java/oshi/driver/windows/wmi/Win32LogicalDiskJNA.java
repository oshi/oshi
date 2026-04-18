/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32LogicalDisk;
import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_LogicalDisk} using JNA.
 */
@ThreadSafe
public final class Win32LogicalDiskJNA extends Win32LogicalDisk {

    private Win32LogicalDiskJNA() {
    }

    /**
     * Queries logical disk information
     *
     * @param nameToMatch an optional string to filter match, null otherwise
     * @param localOnly   Whether to only search local drives
     * @return Logical Disk Information
     */
    public static WmiResult<LogicalDiskProperty> queryLogicalDisk(String nameToMatch, boolean localOnly) {
        WmiQuery<LogicalDiskProperty> logicalDiskQuery = new WmiQuery<>(
                buildWmiClassNameWithWhere(nameToMatch, localOnly), LogicalDiskProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(logicalDiskQuery);
    }
}
