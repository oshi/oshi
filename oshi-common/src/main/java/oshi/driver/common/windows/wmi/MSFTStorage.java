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
        FRIENDLYNAME, OBJECTID;
    }

    /**
     * Properties to link a storage pool with a physical disk. OSHI parses these references to strings that can match
     * the object IDs.
     */
    public enum StoragePoolToPhysicalDiskProperty {
        STORAGEPOOL, PHYSICALDISK;
    }

    /**
     * Properties for a physical disk. The Object ID uniquely defines the disk.
     */
    public enum PhysicalDiskProperty {
        FRIENDLYNAME, PHYSICALLOCATION, OBJECTID;
    }

    /**
     * Properties for a virtual disk. The Object ID uniquely defines the disk.
     */
    public enum VirtualDiskProperty {
        FRIENDLYNAME, OBJECTID;
    }

    protected MSFTStorage() {
    }
}
