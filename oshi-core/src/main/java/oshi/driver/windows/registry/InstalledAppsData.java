/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.software.os.ApplicationInfo;
import oshi.util.ParseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public final class InstalledAppsData {
    private static final Logger LOG = LoggerFactory.getLogger(InstalledAppsData.class);

    private InstalledAppsData() {
    }

    private static final Map<WinReg.HKEY, List<String>> REGISTRY_PATHS = new HashMap<>();

    static {
        REGISTRY_PATHS.put(WinReg.HKEY_LOCAL_MACHINE,
                Arrays.asList("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                        "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));

        REGISTRY_PATHS.put(WinReg.HKEY_CURRENT_USER,
                Arrays.asList("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        List<ApplicationInfo> appInfoList = new ArrayList<>();

        // Iterate through both HKLM and HKCU paths
        for (Map.Entry<WinReg.HKEY, List<String>> entry : REGISTRY_PATHS.entrySet()) {
            WinReg.HKEY rootKey = entry.getKey();
            List<String> uninstallPaths = entry.getValue();

            for (String registryPath : uninstallPaths) {
                for (int accessFlag : new int[] { WinNT.KEY_WOW64_64KEY, WinNT.KEY_WOW64_32KEY }) {
                    String[] keys = Advapi32Util.registryGetKeys(rootKey, registryPath, accessFlag);
                    for (String key : keys) {
                        String fullPath = registryPath + "\\" + key;
                        try {
                            String name = getRegistryValueOrUnknown(rootKey, fullPath, "DisplayName", accessFlag);
                            if (name == null) {
                                continue;
                            }
                            String version = getRegistryValueOrUnknown(rootKey, fullPath, "DisplayVersion", accessFlag);
                            String publisher = getRegistryValueOrUnknown(rootKey, fullPath, "Publisher", accessFlag);
                            String installDate = getRegistryValueOrUnknown(rootKey, fullPath, "InstallDate",
                                    accessFlag);
                            String installLocation = getRegistryValueOrUnknown(rootKey, fullPath, "InstallLocation",
                                    accessFlag);
                            String installSource = getRegistryValueOrUnknown(rootKey, fullPath, "InstallSource",
                                    accessFlag);

                            long installDateEpoch = ParseUtil.parseDateToEpoch(installDate, "yyyyMMdd");

                            Map<String, String> additionalInfo = new HashMap<>();
                            additionalInfo.put("installLocation", installLocation);
                            additionalInfo.put("installSource", installSource);

                            ApplicationInfo app = new ApplicationInfo(name, version, publisher, installDateEpoch,
                                    additionalInfo);
                            appInfoList.add(app);
                        } catch (Win32Exception e) {
                            // Skip keys that are inaccessible or have missing values
                        }
                    }
                }
            }
        }

        return appInfoList;
    }

    private static String getRegistryValueOrUnknown(WinReg.HKEY rootKey, String path, String key, int accessFlag) {
        try {
            String value = Advapi32Util.registryGetStringValue(rootKey, path, key, accessFlag);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        } catch (Win32Exception e) {
            LOG.trace("Unable to access " + path + ": " + e.getMessage());
        }
        return null;
    }
}
