/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskPartition;
import oshi.driver.common.windows.wmi.Win32DiskPartition.DiskPartitionProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class Win32DiskPartitionFFM extends Win32DiskPartition {
    private Win32DiskPartitionFFM() {
    }

    public static WmiResult<DiskPartitionProperty> queryPartition(WmiQueryExecutor h) {
        return Win32DiskPartition.queryPartition(h);
    }
}
