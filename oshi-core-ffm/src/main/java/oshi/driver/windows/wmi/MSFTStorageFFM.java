/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSFTStorage;
import oshi.driver.common.windows.wmi.MSFTStorage.PhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolToPhysicalDiskProperty;
import oshi.driver.common.windows.wmi.MSFTStorage.VirtualDiskProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class MSFTStorageFFM extends MSFTStorage {
    private MSFTStorageFFM() {
    }

    public static WmiResult<StoragePoolProperty> queryStoragePools(WmiQueryExecutor h) {
        return MSFTStorage.queryStoragePools(h);
    }

    public static WmiResult<StoragePoolToPhysicalDiskProperty> queryStoragePoolPhysicalDisks(WmiQueryExecutor h) {
        return MSFTStorage.queryStoragePoolPhysicalDisks(h);
    }

    public static WmiResult<PhysicalDiskProperty> queryPhysicalDisks(WmiQueryExecutor h) {
        return MSFTStorage.queryPhysicalDisks(h);
    }

    public static WmiResult<VirtualDiskProperty> queryVirtualDisks(WmiQueryExecutor h) {
        return MSFTStorage.queryVirtualDisks(h);
    }
}
