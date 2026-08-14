/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.VersionHelpers;

import oshi.driver.common.windows.wmi.MSFTStorage;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.platform.windows.WindowsLogicalVolumeGroup;
import oshi.util.platform.windows.WmiQueryHandler;

final class WindowsLogicalVolumeGroupJNA extends WindowsLogicalVolumeGroup {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsLogicalVolumeGroupJNA.class);

    private static final boolean IS_WINDOWS8_OR_GREATER = VersionHelpers.IsWindows8OrGreater();

    WindowsLogicalVolumeGroupJNA(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        // Storage Spaces requires Windows 8 or Server 2012
        if (!IS_WINDOWS8_OR_GREATER) {
            return Collections.emptyList();
        }
        WmiQueryHandler h = Objects.requireNonNull(WmiQueryHandler.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            // Query Storage Pools first, so we can skip other queries if we have no pools
            WmiResult<StoragePoolProperty> sp = MSFTStorage.queryStoragePools(h);
            if (sp.getResultCount() == 0) {
                return Collections.emptyList();
            }
            return buildFromWmi(sp, MSFTStorage.queryVirtualDisks(h), MSFTStorage.queryPhysicalDisks(h),
                    MSFTStorage.queryStoragePoolPhysicalDisks(h), WindowsLogicalVolumeGroupJNA::new);
        } catch (COMException e) {
            LOG.warn("COM exception", e);
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }
}
