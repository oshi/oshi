/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import java.util.regex.Pattern;

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

    /** Matches a storage pool reference or object ID, capturing the pool's GUID. */
    public static final Pattern SP_OBJECT_ID = Pattern.compile("SP:(\\{[^}]*\\})");
    /** Matches a physical disk reference or object ID, capturing the disk's GUID. */
    public static final Pattern PD_OBJECT_ID = Pattern.compile("PD:(\\{[^}]*\\})");
    /** Matches a virtual disk reference or object ID, capturing its pool's GUID and then its own. */
    public static final Pattern VD_OBJECT_ID = Pattern.compile("VD:(\\{[^}]*\\})(\\{[^}]*\\})");

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
     * The WMI class name for virtual disk to physical disk associations.
     */
    public static final String MSFT_VIRTUAL_DISK_TO_PHYSICAL_DISK = "MSFT_VirtualDiskToPhysicalDisk";

    /**
     * The WMI class name for disks, the storage namespace's view of each disk Windows exposes.
     */
    public static final String MSFT_DISK = "MSFT_Disk";

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
     * Properties for a physical disk's media type. Unlike {@code Win32_DiskDrive}'s MediaType, which reports only
     * removability, this MediaType reports the media technology and so distinguishes an SSD from an HDD.
     */
    public enum PhysicalDiskMediaTypeProperty {
        /** Device ID of the physical disk. For a directly attached disk this is its physical drive number. */
        DEVICEID,
        /** Media type: 3 is HDD, 4 is SSD, 5 is storage class memory, 0 is unspecified. */
        MEDIATYPE,
        /** Spindle speed in RPM. Zero for non-rotational media, {@code UINT32_MAX} if rotating at unknown speed. */
        SPINDLESPEED;
    }

    /**
     * Properties to identify a physical disk by the device number Windows exposes it as.
     */
    public enum PhysicalDiskIdProperty {
        /** Device ID of the physical disk: its physical drive number. */
        DEVICEID,
        /** Object ID of the physical disk. */
        OBJECTID;
    }

    /**
     * Properties to link a virtual disk with the physical disks holding its data. OSHI parses these references to
     * strings that can match the object IDs.
     */
    public enum VirtualDiskToPhysicalDiskProperty {
        /** Virtual disk reference. */
        VIRTUALDISK,
        /** Physical disk reference. */
        PHYSICALDISK;
    }

    /**
     * Properties for a disk, to identify what it is built on.
     */
    public enum DiskProperty {
        /** The disk number, matching {@code Win32_DiskDrive}'s Index. */
        NUMBER,
        /** The bus type: 15 is a file-backed virtual disk, 16 a Storage Spaces disk. */
        BUSTYPE,
        /** For a file-backed virtual disk, the path of its backing file. */
        LOCATION,
        /** The unique ID, which for a Storage Spaces disk matches its virtual disk's. */
        UNIQUEID;
    }

    /**
     * Properties for a virtual disk's identity, to match it with the disk Windows exposes it as.
     */
    public enum VirtualDiskIdProperty {
        /** The unique ID, matching the {@code MSFT_Disk} exposing this virtual disk. */
        UNIQUEID,
        /** Object ID of the virtual disk. */
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
     * Queries physical disk media types. Requires Windows 8 or Server 2012.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Physical disk media type information.
     */
    public static WmiResult<PhysicalDiskMediaTypeProperty> queryPhysicalDiskMediaTypes(WmiQueryExecutor h) {
        WmiQuery<PhysicalDiskMediaTypeProperty> query = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_PHYSICAL_DISK,
                PhysicalDiskMediaTypeProperty.class);
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

    /**
     * Queries disks. Requires Windows 8 or Server 2012.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Disk information.
     */
    public static WmiResult<DiskProperty> queryDisks(WmiQueryExecutor h) {
        WmiQuery<DiskProperty> query = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_DISK, DiskProperty.class);
        return h.queryWMI(query, false);
    }

    /**
     * Queries virtual disk identities.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Virtual disk identity information.
     */
    public static WmiResult<VirtualDiskIdProperty> queryVirtualDiskIds(WmiQueryExecutor h) {
        WmiQuery<VirtualDiskIdProperty> query = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_VIRTUAL_DISK,
                VirtualDiskIdProperty.class);
        return h.queryWMI(query, false);
    }

    /**
     * Queries the virtual disk to physical disk associations.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return The associations.
     */
    public static WmiResult<VirtualDiskToPhysicalDiskProperty> queryVirtualDiskToPhysicalDisks(WmiQueryExecutor h) {
        WmiQuery<VirtualDiskToPhysicalDiskProperty> query = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_VIRTUAL_DISK_TO_PHYSICAL_DISK, VirtualDiskToPhysicalDiskProperty.class);
        return h.queryWMI(query, false);
    }

    /**
     * Queries physical disk identities.
     *
     * @param h An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @return Physical disk identity information.
     */
    public static WmiResult<PhysicalDiskIdProperty> queryPhysicalDiskIds(WmiQueryExecutor h) {
        WmiQuery<PhysicalDiskIdProperty> query = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_PHYSICAL_DISK,
                PhysicalDiskIdProperty.class);
        return h.queryWMI(query, false);
    }
}
