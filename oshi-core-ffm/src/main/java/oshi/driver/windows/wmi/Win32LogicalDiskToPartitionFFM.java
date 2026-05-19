/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition.DiskToPartitionProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class Win32LogicalDiskToPartitionFFM extends Win32LogicalDiskToPartition {
    private Win32LogicalDiskToPartitionFFM() {
    }

    public static WmiResult<DiskToPartitionProperty> queryDiskToPartition(WmiQueryExecutor h) {
        return Win32LogicalDiskToPartition.queryDiskToPartition(h);
    }
}
