/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.windows;

import oshi.ffm.driver.windows.registry.InstalledAppsDataFFM;
import oshi.software.os.ApplicationInfo;

import java.util.List;

public final class WindowsInstalledAppsFFM {

    private WindowsInstalledAppsFFM() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        return InstalledAppsDataFFM.queryInstalledApps();
    }

}
