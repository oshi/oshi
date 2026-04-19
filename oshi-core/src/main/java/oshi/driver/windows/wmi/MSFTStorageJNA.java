/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSFTStorage;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI classes in Storage namespace associated with Storage Pools using JNA.
 */
@ThreadSafe
public final class MSFTStorageJNA extends MSFTStorage {

    private MSFTStorageJNA() {
    }

    /**
     * Query the storage pools.
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return Storage pools that are not primordial (raw disks not added to a storage space).
     */
    public static WmiResult<StoragePoolProperty> queryStoragePools(WmiQueryHandler h) {
        WmiQuery<StoragePoolProperty> storagePoolQuery = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE, StoragePoolProperty.class);
        return h.queryWMI(storagePoolQuery, false);
    }

    /**
     * Query the storage pool to physical disk connection.
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return Links between physical disks and storage pools.
     */
    public static WmiResult<StoragePoolToPhysicalDiskProperty> queryStoragePoolPhysicalDisks(WmiQueryHandler h) {
        WmiQuery<StoragePoolToPhysicalDiskProperty> storagePoolToPhysicalDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_TO_PHYSICAL_DISK, StoragePoolToPhysicalDiskProperty.class);
        return h.queryWMI(storagePoolToPhysicalDiskQuery, false);
    }

    /**
     * Query the physical disks.
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return The physical disks.
     */
    public static WmiResult<PhysicalDiskProperty> queryPhysicalDisks(WmiQueryHandler h) {
        WmiQuery<PhysicalDiskProperty> physicalDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_PHYSICAL_DISK,
                PhysicalDiskProperty.class);
        return h.queryWMI(physicalDiskQuery, false);
    }

    /**
     * Query the virtual disks.
     *
     * @param h An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @return The virtual disks.
     */
    public static WmiResult<VirtualDiskProperty> queryVirtualDisks(WmiQueryHandler h) {
        WmiQuery<VirtualDiskProperty> virtualDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_VIRTUAL_DISK,
                VirtualDiskProperty.class);
        return h.queryWMI(virtualDiskQuery, false);
    }
}
