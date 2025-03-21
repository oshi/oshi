/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.driver.windows.registry.InstalledAppsData;
import oshi.software.os.ApplicationInfo;

import java.util.List;

public class WindowsInstalledApps {

    private WindowsInstalledApps() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        return InstalledAppsData.queryInstalledApps();
    }
}
