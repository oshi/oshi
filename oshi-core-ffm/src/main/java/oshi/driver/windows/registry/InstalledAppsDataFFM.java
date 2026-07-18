/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static oshi.ffm.platform.windows.Advapi32FFM.RegCloseKey;
import static oshi.ffm.platform.windows.Advapi32FFM.RegOpenKeyEx;
import static oshi.ffm.platform.windows.WinNTFFM.KEY_READ;
import static oshi.ffm.platform.windows.WinNTFFM.KEY_WOW64_32KEY;
import static oshi.ffm.platform.windows.WinNTFFM.KEY_WOW64_64KEY;
import static oshi.ffm.platform.windows.WinRegFFM.HKEY_CURRENT_USER;
import static oshi.ffm.platform.windows.WinRegFFM.HKEY_LOCAL_MACHINE;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.checkSuccess;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.toWideString;
import static oshi.ffm.util.platform.windows.Advapi32UtilFFM.registryGetValue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.driver.common.windows.registry.RegistryValueUtil;
import oshi.ffm.platform.windows.Win32Exception;
import oshi.ffm.util.platform.windows.Advapi32UtilFFM;
import oshi.software.os.ApplicationInfo;

public final class InstalledAppsDataFFM {

    private static final Logger LOG = LoggerFactory.getLogger(InstalledAppsDataFFM.class);

    private static final Map<MemorySegment, List<String>> REGISTRY_PATHS = new LinkedHashMap<>();
    private static final int[] ACCESS_FLAGS = { KEY_WOW64_64KEY, KEY_WOW64_32KEY };

    static {
        REGISTRY_PATHS.put(MemorySegment.ofAddress(HKEY_LOCAL_MACHINE),
                Arrays.asList("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                        "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
        REGISTRY_PATHS.put(MemorySegment.ofAddress(HKEY_CURRENT_USER),
                Arrays.asList("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"));
    }

    private InstalledAppsDataFFM() {
    }

    public static List<ApplicationInfo> queryInstalledApps() {
        Set<ApplicationInfo> appInfoSet = new LinkedHashSet<>();

        for (Map.Entry<MemorySegment, List<String>> entry : REGISTRY_PATHS.entrySet()) {
            MemorySegment rootKey = entry.getKey();
            List<String> uninstallPaths = entry.getValue();

            for (String registryPath : uninstallPaths) {
                for (int accessFlag : ACCESS_FLAGS) {
                    try {
                        String[] keys = Advapi32UtilFFM.registryGetKeys(rootKey, registryPath, accessFlag);
                        for (String key : keys) {
                            String fullPath = registryPath + "\\" + key;
                            try {
                                String name = RegistryValueUtil.registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "DisplayName", accessFlag));
                                if (name == null) {
                                    continue;
                                }
                                String version = RegistryValueUtil.registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "DisplayVersion", accessFlag));
                                String publisher = RegistryValueUtil.registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "Publisher", accessFlag));
                                long installDate = RegistryValueUtil.registryValueToLong(
                                        getRegistryValueOrNull(rootKey, fullPath, "InstallDate", accessFlag));
                                String installLocation = RegistryValueUtil.registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "InstallLocation", accessFlag));
                                String installSource = RegistryValueUtil.registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "InstallSource", accessFlag));

                                Map<String, String> additionalInfo = new LinkedHashMap<>();
                                additionalInfo.put("installLocation", installLocation);
                                additionalInfo.put("installSource", installSource);

                                ApplicationInfo app = new ApplicationInfo(name, version, publisher, installDate,
                                        additionalInfo);
                                appInfoSet.add(app);

                            } catch (Throwable e) {
                                LOG.trace("Skipping key {}: {}", fullPath, e.getMessage());
                            }
                        }
                    } catch (Throwable e) {
                        LOG.trace("Skipping path {}: {}", registryPath, e.getMessage());
                    }
                }
            }
        }

        return new ArrayList<>(appInfoSet);
    }

    private static Object getRegistryValueOrNull(MemorySegment rootKey, String path, String key, int accessFlag)
            throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment phkKey = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, path), 0, KEY_READ | accessFlag, phkKey);
            checkSuccess(rc);

            MemorySegment hKey = phkKey.get(ADDRESS, 0);
            try {
                return registryGetValue(hKey, key);
            } finally {
                rc = RegCloseKey(hKey);
                checkSuccess(rc);
            }
        } catch (Win32Exception e) {
            LOG.trace("Unable to access {} with flag {}: {}", path, accessFlag, e.getMessage());
            return null;
        }
    }
}
