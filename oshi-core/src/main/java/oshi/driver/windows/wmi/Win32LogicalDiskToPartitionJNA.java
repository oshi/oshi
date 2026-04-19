/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_LogicalDiskToPartition} using JNA.
 */
@ThreadSafe
public final class Win32LogicalDiskToPartitionJNA extends Win32LogicalDiskToPartition {

    private Win32LogicalDiskToPartitionJNA() {
    }

    /**
     * Queries the association between logical disk and partition.
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return Antecedent-dependent pairs of disk and partition.
     */
    public static WmiResult<DiskToPartitionProperty> queryDiskToPartition(WmiQueryHandler h) {
        WmiQuery<DiskToPartitionProperty> diskToPartitionQuery = new WmiQuery<>(WIN32_LOGICAL_DISK_TO_PARTITION,
                DiskToPartitionProperty.class);
        return h.queryWMI(diskToPartitionQuery, false);
    }
}
