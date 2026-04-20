/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.List;

import oshi.driver.windows.registry.InstalledAppsData;
import oshi.software.os.ApplicationInfo;

public final class WindowsInstalledAppsJNA {

    private WindowsInstalledAppsJNA() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        return InstalledAppsData.queryInstalledApps();
    }
}
