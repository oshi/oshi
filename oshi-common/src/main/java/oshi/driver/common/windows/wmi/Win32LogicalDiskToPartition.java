/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_LogicalDiskToPartition}.
 */
@ThreadSafe
public class Win32LogicalDiskToPartition {

    /**
     * The WMI class name.
     */
    public static final String WIN32_LOGICAL_DISK_TO_PARTITION = "Win32_LogicalDiskToPartition";

    /**
     * Links disk drives to partitions.
     */
    public enum DiskToPartitionProperty {
        /** ANTECEDENT property. */
        ANTECEDENT,
        /** DEPENDENT property. */
        DEPENDENT,
        /** ENDINGADDRESS property. */
        ENDINGADDRESS,
        /** STARTINGADDRESS property. */
        STARTINGADDRESS;
    }

    /**
     * Constructor.
     */
    protected Win32LogicalDiskToPartition() {
    }

    /**
     * Queries logical disk to partition mapping.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Logical disk to partition mapping.
     */
    public static WmiResult<DiskToPartitionProperty> queryDiskToPartition(WmiQueryExecutor h) {
        WmiQuery<DiskToPartitionProperty> query = new WmiQuery<>(WIN32_LOGICAL_DISK_TO_PARTITION,
                DiskToPartitionProperty.class);
        return h.queryWMI(query, false);
    }
}
