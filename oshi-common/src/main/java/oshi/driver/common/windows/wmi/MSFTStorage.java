/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enums for WMI classes in the Storage namespace associated with Storage Pools.
 */
@ThreadSafe
public class MSFTStorage {

    /**
     * The WMI namespace for storage.
     */
    public static final String STORAGE_NAMESPACE = "ROOT\\Microsoft\\Windows\\Storage";

    /**
     * The WMI class name for non-primordial storage pools.
     */
    public static final String MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE = "MSFT_StoragePool WHERE IsPrimordial=FALSE";

    /**
     * The WMI class name for storage pool to physical disk associations.
     */
    public static final String MSFT_STORAGE_POOL_TO_PHYSICAL_DISK = "MSFT_StoragePoolToPhysicalDisk";

    /**
     * The WMI class name for physical disks.
     */
    public static final String MSFT_PHYSICAL_DISK = "MSFT_PhysicalDisk";

    /**
     * The WMI class name for virtual disks.
     */
    public static final String MSFT_VIRTUAL_DISK = "MSFT_VirtualDisk";

    /**
     * Properties to identify the storage pool. The Object ID uniquely defines the pool.
     */
    public enum StoragePoolProperty {
        /** Friendly name of the storage pool. */
        FRIENDLYNAME,
        /** Object ID of the storage pool. */
        OBJECTID;
    }

    /**
     * Properties to link a storage pool with a physical disk. OSHI parses these references to strings that can match
     * the object IDs.
     */
    public enum StoragePoolToPhysicalDiskProperty {
        /** Storage pool reference. */
        STORAGEPOOL,
        /** Physical disk reference. */
        PHYSICALDISK;
    }

    /**
     * Properties for a physical disk. The Object ID uniquely defines the disk.
     */
    public enum PhysicalDiskProperty {
        /** Friendly name of the physical disk. */
        FRIENDLYNAME,
        /** Physical location of the disk. */
        PHYSICALLOCATION,
        /** Object ID of the physical disk. */
        OBJECTID;
    }

    /**
     * Properties for a virtual disk. The Object ID uniquely defines the disk.
     */
    public enum VirtualDiskProperty {
        /** Friendly name of the virtual disk. */
        FRIENDLYNAME,
        /** Object ID of the virtual disk. */
        OBJECTID;
    }

    /**
     * Constructor.
     */
    protected MSFTStorage() {
    }

    /**
     * Queries storage pools.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Storage pool information.
     */
    public static WmiResult<StoragePoolProperty> queryStoragePools(WmiQueryExecutor h) {
        WmiQuery<StoragePoolProperty> query = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE, StoragePoolProperty.class);
        return h.queryWMI(query, false);
    }

    /**
     * Queries storage pool to physical disk mapping.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Storage pool to physical disk mapping.
     */
    public static WmiResult<StoragePoolToPhysicalDiskProperty> queryStoragePoolPhysicalDisks(WmiQueryExecutor h) {
        WmiQuery<StoragePoolToPhysicalDiskProperty> query = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_TO_PHYSICAL_DISK, StoragePoolToPhysicalDiskProperty.class);
        return h.queryWMI(query, false);
    }

    /**
     * Queries physical disks.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Physical disk information.
     */
    public static WmiResult<PhysicalDiskProperty> queryPhysicalDisks(WmiQueryExecutor h) {
        WmiQuery<PhysicalDiskProperty> query = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_PHYSICAL_DISK,
                PhysicalDiskProperty.class);
        return h.queryWMI(query, false);
    }

    /**
     * Queries virtual disks.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Virtual disk information.
     */
    public static WmiResult<VirtualDiskProperty> queryVirtualDisks(WmiQueryExecutor h) {
        WmiQuery<VirtualDiskProperty> query = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_VIRTUAL_DISK,
                VirtualDiskProperty.class);
        return h.queryWMI(query, false);
    }
}
