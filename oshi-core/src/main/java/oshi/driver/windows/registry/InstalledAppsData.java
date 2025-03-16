/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import oshi.util.ExecutingCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class InstalledAppsData {
    private InstalledAppsData() {
    }

    private static final List<String> UNINSTALL_PATHS = Arrays.asList(
            "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKLM\\Software\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall");

    public enum AppInfo {
        NAME, VERSION, VENDOR, INSTALLDATE, INSTALLLOCATION, INSTALLSOURCE;
    }

    public static List<Map<AppInfo, String>> queryInstalledApps() {
        List<Map<AppInfo, String>> appInfoList = new ArrayList<>();

        for (String path : UNINSTALL_PATHS) {
            List<String> output = ExecutingCommand.runNative("reg query " + path + " /s");
            parseRegistryOutput(output, appInfoList);
        }

        return appInfoList;
    }

    private static void parseRegistryOutput(List<String> output, List<Map<AppInfo, String>> appInfoList) {
        Map<AppInfo, String> currentApp = new HashMap<>();

        for (String line : output) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("HKEY")) {
                if (!currentApp.isEmpty()) {
                    appInfoList.add(new HashMap<>(currentApp));
                    currentApp.clear();
                }
            } else {
                String[] parts = line.trim().split("\\s{4,}");

                if (parts.length > 2) {
                    String key = parts[0].trim();
                    String value = parts[2].trim();

                    switch (key) {
                    case "DisplayName":
                        currentApp.put(AppInfo.NAME, value);
                        break;
                    case "DisplayVersion":
                        currentApp.put(AppInfo.VERSION, value);
                        break;
                    case "Publisher":
                        currentApp.put(AppInfo.VENDOR, value);
                        break;
                    case "InstallDate":
                        currentApp.put(AppInfo.INSTALLDATE, value);
                        break;
                    case "InstallLocation":
                        currentApp.put(AppInfo.INSTALLLOCATION, value);
                        break;
                    case "InstallSource":
                        currentApp.put(AppInfo.INSTALLSOURCE, value);
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        if (!currentApp.isEmpty()) {
            appInfoList.add(currentApp);
        }
    }
}
