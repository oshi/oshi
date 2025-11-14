/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static com.sun.jna.platform.win32.WinNT.KEY_WOW64_32KEY;
import static com.sun.jna.platform.win32.WinNT.KEY_WOW64_64KEY;
import static com.sun.jna.platform.win32.WinReg.HKEY_CURRENT_USER;
import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg.HKEY;

import oshi.software.os.ApplicationInfo;
import oshi.util.platform.windows.RegistryUtil;

public final class InstalledAppsData {
    private static final Logger LOG = LoggerFactory.getLogger(InstalledAppsData.class);

    private static final Advapi32 ADV = Advapi32.INSTANCE;

    private static final long THIRTY_YEARS_IN_SECS = 30L * 365 * 24 * 60 * 60;

    private InstalledAppsData() {
    }

    private static final Map<HKEY, List<String>> REGISTRY_PATHS = new HashMap<>();
    private static final int[] ACCESS_FLAGS = { KEY_WOW64_64KEY, KEY_WOW64_32KEY };

    static {
        REGISTRY_PATHS.put(HKEY_LOCAL_MACHINE, Arrays.asList("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));

        REGISTRY_PATHS.put(HKEY_CURRENT_USER, Arrays.asList("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();

        // Iterate through both HKLM and HKCU paths
        for (Map.Entry<HKEY, List<String>> entry : REGISTRY_PATHS.entrySet()) {
            HKEY rootKey = entry.getKey();
            List<String> uninstallPaths = entry.getValue();

            for (String registryPath : uninstallPaths) {
                for (int accessFlag : ACCESS_FLAGS) {
                    try {
                        String[] keys = Advapi32Util.registryGetKeys(rootKey, registryPath, accessFlag);
                        for (String key : keys) {
                            String fullPath = registryPath + "\\" + key;
                            try {
                                String name = RegistryUtil.getStringValue(rootKey, fullPath, "DisplayName", accessFlag);
                                if (name == null) {
                                    continue;
                                }
                                String version = RegistryUtil.getStringValue(rootKey, fullPath, "DisplayVersion",
                                        accessFlag);
                                String publisher = RegistryUtil.getStringValue(rootKey, fullPath, "Publisher",
                                        accessFlag);
                                long installDate = RegistryUtil.getLongValue(rootKey, fullPath, "InstallDate",
                                        accessFlag);
                                String installLocation = RegistryUtil.getStringValue(rootKey, fullPath,
                                        "InstallLocation", accessFlag);
                                String installSource = RegistryUtil.getStringValue(rootKey, fullPath, "InstallSource",
                                        accessFlag);

                                Map<String, String> additionalInfo = new LinkedHashMap<>();
                                additionalInfo.put("installLocation", installLocation);
                                additionalInfo.put("installSource", installSource);

                                ApplicationInfo app = new ApplicationInfo(name, version, publisher, installDate,
                                        additionalInfo);
                                appInfoSet.add(app);
                            } catch (Win32Exception e) {
                                // Skip keys that are inaccessible or have missing values
                            }
                        }
                    } catch (Win32Exception e) {
                        // Skip paths that are inaccessible
                    }
                }
            }
        }

        return new ArrayList<>(appInfoSet);
    }

}
