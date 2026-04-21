/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32LogicalDiskToPartition;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_LogicalDiskToPartition} using FFM.
 */
@ThreadSafe
public final class Win32LogicalDiskToPartitionFFM extends Win32LogicalDiskToPartition {

    private Win32LogicalDiskToPartitionFFM() {
    }

    /**
     * Queries the association between logical disk and partition.
     *
     * @param h An instantiated {@link WmiQueryHandlerFFM}. User should have already initialized COM.
     * @return Antecedent-dependent pairs of disk and partition.
     */
    public static WmiResult<DiskToPartitionProperty> queryDiskToPartition(WmiQueryHandlerFFM h) {
        WmiQuery<DiskToPartitionProperty> diskToPartitionQuery = new WmiQuery<>(WIN32_LOGICAL_DISK_TO_PARTITION,
                DiskToPartitionProperty.class);
        return h.queryWMI(diskToPartitionQuery, false);
    }
}
