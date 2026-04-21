/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import java.util.List;

import oshi.ffm.driver.windows.registry.InstalledAppsDataFFM;
import oshi.software.os.ApplicationInfo;

public final class WindowsInstalledAppsFFM {

    private WindowsInstalledAppsFFM() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        return InstalledAppsDataFFM.queryInstalledApps();
    }

}
