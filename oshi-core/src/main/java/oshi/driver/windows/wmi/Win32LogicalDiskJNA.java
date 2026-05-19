/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32LogicalDisk;
import oshi.driver.common.windows.wmi.Win32LogicalDisk.LogicalDiskProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32LogicalDiskJNA extends Win32LogicalDisk {
    private Win32LogicalDiskJNA() {
    }

    public static WmiResult<LogicalDiskProperty> queryLogicalDisk(String nameToMatch, boolean localOnly) {
        return Win32LogicalDisk.queryLogicalDisk(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()),
                nameToMatch, localOnly);
    }
}
