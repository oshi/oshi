/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

public class InstalledAppsData {
    private InstalledAppsData() {
    }

    private static final Map<WinReg.HKEY, List<String>> REGISTRY_PATHS;

    static {
        REGISTRY_PATHS = new HashMap<>();

        List<String> hklmPaths = new ArrayList<>();
        hklmPaths.add("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
        hklmPaths.add("SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
        REGISTRY_PATHS.put(WinReg.HKEY_LOCAL_MACHINE, hklmPaths);

        List<String> hkcuPaths = new ArrayList<>();
        hkcuPaths.add("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall");
        REGISTRY_PATHS.put(WinReg.HKEY_CURRENT_USER, hkcuPaths);
    }

    public enum AppInfo {
        NAME, VERSION, VENDOR, INSTALLDATE, INSTALLLOCATION, INSTALLSOURCE;
    }

    public static List<Map<AppInfo, String>> queryInstalledApps() {
        List<Map<AppInfo, String>> appInfoList = new ArrayList<>();

        // Iterate through both HKLM and HKCU paths
        for (Map.Entry<WinReg.HKEY, List<String>> entry : REGISTRY_PATHS.entrySet()) {
            WinReg.HKEY rootKey = entry.getKey();
            List<String> uninstallPaths = entry.getValue();

            for (String registryPath : uninstallPaths) {
                String[] keys = Advapi32Util.registryGetKeys(rootKey, registryPath);

                for (String key : keys) {
                    String fullPath = registryPath + "\\" + key;
                    try {
                        Map<AppInfo, String> appDetails = new EnumMap<>(AppInfo.class);
                        appDetails.put(AppInfo.NAME, Advapi32Util.registryGetStringValue(rootKey, fullPath, "DisplayName"));
                        appDetails.put(AppInfo.VERSION, Advapi32Util.registryGetStringValue(rootKey, fullPath, "DisplayVersion"));
                        appDetails.put(AppInfo.VENDOR, Advapi32Util.registryGetStringValue(rootKey, fullPath, "Publisher"));
                        appDetails.put(AppInfo.INSTALLDATE, Advapi32Util.registryGetStringValue(rootKey, fullPath, "InstallDate"));
                        appDetails.put(AppInfo.INSTALLLOCATION, Advapi32Util.registryGetStringValue(rootKey, fullPath, "InstallLocation"));
                        appDetails.put(AppInfo.INSTALLSOURCE, Advapi32Util.registryGetStringValue(rootKey, fullPath, "InstallSource"));
                        appInfoList.add(appDetails);
                    } catch (Win32Exception e) {
                        // Skip keys that are inaccessible or have missing values
                    }
                }
            }
        }

        return appInfoList;
    }
}
