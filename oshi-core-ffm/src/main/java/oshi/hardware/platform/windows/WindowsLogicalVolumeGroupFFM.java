/*
 * Copyright 2026 The OSHI Project Contributors
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

import oshi.driver.common.windows.wmi.MSFTStorage;
import oshi.driver.common.windows.wmi.MSFTStorage.StoragePoolProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.platform.windows.VersionHelpersFFM;
import oshi.ffm.platform.windows.com.FfmComException;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;
import oshi.hardware.LogicalVolumeGroup;
import oshi.hardware.common.platform.windows.WindowsLogicalVolumeGroup;

final class WindowsLogicalVolumeGroupFFM extends WindowsLogicalVolumeGroup {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsLogicalVolumeGroupFFM.class);

    private static final boolean IS_WINDOWS8_OR_GREATER = VersionHelpersFFM.IsWindows8OrGreater();

    WindowsLogicalVolumeGroupFFM(String name, Map<String, Set<String>> lvMap, Set<String> pvSet) {
        super(name, lvMap, pvSet);
    }

    static List<LogicalVolumeGroup> getLogicalVolumeGroups() {
        // Storage Spaces requires Windows 8 or Server 2012
        if (!IS_WINDOWS8_OR_GREATER) {
            return Collections.emptyList();
        }
        WmiQueryHandlerFFM h = Objects.requireNonNull(WmiQueryHandlerFFM.createInstance());
        boolean comInit = false;
        try {
            comInit = h.initCOM();
            // Query Storage Pools first, so we can skip other queries if we have no pools
            WmiResult<StoragePoolProperty> sp = MSFTStorage.queryStoragePools(h);
            if (sp.getResultCount() == 0) {
                return Collections.emptyList();
            }
            return buildFromWmi(sp, MSFTStorage.queryVirtualDisks(h), MSFTStorage.queryPhysicalDisks(h),
                    MSFTStorage.queryStoragePoolPhysicalDisks(h), WindowsLogicalVolumeGroupFFM::new);
        } catch (FfmComException e) {
            LOG.warn("COM exception: {}", e.getMessage());
            return Collections.emptyList();
        } finally {
            if (comInit) {
                h.unInitCOM();
            }
        }
    }
}
