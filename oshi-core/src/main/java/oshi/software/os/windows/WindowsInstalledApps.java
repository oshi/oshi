/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.driver.windows.registry.InstalledAppsData;
import oshi.software.os.ApplicationInfo;

import java.util.Set;

public final class WindowsInstalledApps {

    private WindowsInstalledApps() {
    }

    public static Set<ApplicationInfo> queryInstalledApps() {
        return InstalledAppsData.queryInstalledApps();
    }
}
