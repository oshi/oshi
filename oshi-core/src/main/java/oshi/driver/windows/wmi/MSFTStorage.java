/**
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
 * Utility to query WMI classes in Storage namespace
 */
@ThreadSafe
public final class MSFTStorage {

    private static final String STORAGE_NAMESPACE = "ROOT\\Microsoft\\Windows\\Storage";
    private static final String MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE = "MSFT_StoragePool WHERE IsPrimordial=FALSE";
    private static final String MSFT_STORAGE_POOL_TO_PHYSICAL_DISK = "MSFT_StoragePoolToPhysicalDisk";
    private static final String MSFT_PHYSICAL_DISK = "MSFT_PhysicalDisk";
    private static final String MSFT_VIRTUAL_DISK = "MSFT_VirtualDisk";

    public enum StoragePoolProperty {
        FRIENDLYNAME, OBJECTID;
    }

    public enum StoragePoolToPhysicalDiskProperty {
        STORAGEPOOL, PHYSICALDISK;
    }

    public enum PhysicalDiskProperty {
        FRIENDLYNAME, PHYSICALLOCATION, OBJECTID;
    }

    public enum VirtualDiskProperty {
        FRIENDLYNAME, OBJECTID;
    }

    private MSFTStorage() {
    }

    public static WmiResult<StoragePoolProperty> queryStoragePools() {
        WmiQuery<StoragePoolProperty> storagePoolQuery = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_WHERE_IS_PRIMORDIAL_FALSE, StoragePoolProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(storagePoolQuery);
    }

    public static WmiResult<StoragePoolToPhysicalDiskProperty> queryStoragePoolPhysicalDisks() {
        WmiQuery<StoragePoolToPhysicalDiskProperty> storagePoolToPhysicalDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE,
                MSFT_STORAGE_POOL_TO_PHYSICAL_DISK, StoragePoolToPhysicalDiskProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(storagePoolToPhysicalDiskQuery);
    }

    public static WmiResult<PhysicalDiskProperty> queryPhysicalDisks() {
        WmiQuery<PhysicalDiskProperty> physicalDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_PHYSICAL_DISK,
                PhysicalDiskProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(physicalDiskQuery);
    }

    public static WmiResult<VirtualDiskProperty> queryVirtualDisks() {
        WmiQuery<VirtualDiskProperty> virtualDiskQuery = new WmiQuery<>(STORAGE_NAMESPACE, MSFT_VIRTUAL_DISK,
                VirtualDiskProperty.class);
        return WmiQueryHandler.createInstance().queryWMI(virtualDiskQuery);
    }

}
