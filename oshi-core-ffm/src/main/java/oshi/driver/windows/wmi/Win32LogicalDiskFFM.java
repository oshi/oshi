/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32LogicalDisk;
import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_LogicalDisk} using FFM.
 */
@ThreadSafe
public final class Win32LogicalDiskFFM extends Win32LogicalDisk {

    private Win32LogicalDiskFFM() {
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
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(logicalDiskQuery);
    }
}
