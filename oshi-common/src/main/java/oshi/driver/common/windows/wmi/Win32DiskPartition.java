/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_DiskPartition}.
 */
@ThreadSafe
public class Win32DiskPartition {

    /**
     * The WMI class name.
     */
    public static final String WIN32_DISK_PARTITION = "Win32_DiskPartition";

    /**
     * Disk partition properties.
     */
    public enum DiskPartitionProperty {
        /** INDEX property. */
        INDEX,
        /** DESCRIPTION property. */
        DESCRIPTION,
        /** DEVICEID property. */
        DEVICEID,
        /** DISKINDEX property. */
        DISKINDEX,
        /** NAME property. */
        NAME,
        /** SIZE property. */
        SIZE,
        /** TYPE property. */
        TYPE;
    }

    /**
     * Constructor.
     */
    protected Win32DiskPartition() {
    }

    /**
     * Queries disk partition information.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Disk partition information.
     */
    public static WmiResult<DiskPartitionProperty> queryPartition(WmiQueryExecutor h) {
        WmiQuery<DiskPartitionProperty> query = new WmiQuery<>(WIN32_DISK_PARTITION, DiskPartitionProperty.class);
        return h.queryWMI(query, false);
    }
}
