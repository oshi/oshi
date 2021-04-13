/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery; //NOSONAR squid:S1191
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI classes in Storage namespace assocaited with Storage
 * Pools
 */
@ThreadSafe
public final class MSFTStorage {

    private static final String STORAGE_NAMESPACE = "ROOT\\Microsoft\\Windows\\Storage";
    private static final String MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE = "MSFT_StoragePool WHERE IsPrimordial=FALSE";
    private static final String MSFT_STORAGE_POOL_TO_PHYSICAL_DISK = "MSFT_StoragePoolToPhysicalDisk";
    private static final String MSFT_PHYSICAL_DISK = "MSFT_PhysicalDisk";
    private static final String MSFT_VIRTUAL_DISK = "MSFT_VirtualDisk";

    /**
     * Properties to identify the storage pool. The Object ID uniquely defines the
     * pool.
     */
    public enum StoragePoolProperty {
        FRIENDLYNAME, OBJECTID;
    }

    /**
     * Properties to link a storage pool with a physical disk. OSHI parses these
     * references to strings that can match the object IDs.
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

    private MSFTStorage() {
    }

    /**
     * Query the storage pools.
     *
     * @param h
     *            An instantiated {@link WmiQueryHandler}. User should have already
     *            initialized COM.
     * @return Storage pools that are not primordial (raw disks not added to a
     *         storage space).
     */
    public static WmiResult<StoragePoolProperty> queryStoragePools(WmiQueryHandler h) {
        WmiQuery<StoragePoolProperty> storagePoolQuery = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE, StoragePoolProperty.class);
        return h.queryWMI(storagePoolQuery, false);
    }

    /**
     * Query the storage pool to physical disk connection.
     *
     * @param h
     *            An instantiated {@link WmiQueryHandler}. User should have already
     *            initialized COM.
     * @return Links between physical disks and storage pools. All raw disks will be
     *         part of the primordial pool in addition to the storage space they are
     *         a member of.
     */
    public static WmiResult<StoragePoolToPhysicalDiskProperty> queryStoragePoolPhysicalDisks(WmiQueryHandler h) {
        WmiQuery<StoragePoolToPhysicalDiskProperty> storagePoolToPhysicalDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_TO_PHYSICAL_DISK, StoragePoolToPhysicalDiskProperty.class);
        return h.queryWMI(storagePoolToPhysicalDiskQuery, false);
    }

    /**
     * Query the physical disks.
     *
     * @param h
     *            An instantiated {@link WmiQueryHandler}. User should have already
     *            initialized COM.
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
     * @param h
     *            An instantiated {@link WmiQueryHandler}. User should have already
     *            initialized COM.
     * @return The virtual disks.
     */
    public static WmiResult<VirtualDiskProperty> queryVirtualDisks(WmiQueryHandler h) {
        WmiQuery<VirtualDiskProperty> virtualDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_VIRTUAL_DISK,
                VirtualDiskProperty.class);
        return h.queryWMI(virtualDiskQuery, false);
    }

}
