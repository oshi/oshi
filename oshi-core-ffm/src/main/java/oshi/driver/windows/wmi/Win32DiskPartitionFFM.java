/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32DiskPartition;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_DiskPartition} using FFM.
 */
@ThreadSafe
public final class Win32DiskPartitionFFM extends Win32DiskPartition {

    private Win32DiskPartitionFFM() {
    }

    /**
     * Queries the partition.
     *
     * @param h An instantiated {@link WmiQueryHandlerFFM}. User should have already initialized COM.
     * @return Information regarding each disk partition.
     */
    public static WmiResult<DiskPartitionProperty> queryPartition(WmiQueryHandlerFFM h) {
        WmiQuery<DiskPartitionProperty> partitionQuery = new WmiQuery<>(WIN32_DISK_PARTITION,
                DiskPartitionProperty.class);
        return h.queryWMI(partitionQuery, false);
    }
}
