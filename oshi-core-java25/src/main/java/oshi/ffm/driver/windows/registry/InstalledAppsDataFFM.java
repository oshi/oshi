/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.driver.windows.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.software.os.ApplicationInfo;
import oshi.util.ParseUtil;
import oshi.ffm.windows.Win32Exception;
import oshi.util.platform.windows.Advapi32UtilFFM;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.Advapi32FFM.RegCloseKey;
import static oshi.ffm.windows.Advapi32FFM.RegOpenKeyEx;
import static oshi.ffm.windows.Advapi32FFM.RegQueryValueEx;
import static oshi.ffm.windows.WinErrorFFM.ERROR_INSUFFICIENT_BUFFER;
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.WinNTFFM.KEY_READ;
import static oshi.ffm.windows.WinNTFFM.KEY_WOW64_32KEY;
import static oshi.ffm.windows.WinNTFFM.KEY_WOW64_64KEY;
import static oshi.ffm.windows.WinNTFFM.REG_SZ;
import static oshi.ffm.windows.WinNTFFM.REG_DWORD;
import static oshi.ffm.windows.WinNTFFM.REG_EXPAND_SZ;
import static oshi.ffm.windows.WinRegFFM.HKEY_CURRENT_USER;
import static oshi.ffm.windows.WinRegFFM.HKEY_LOCAL_MACHINE;
import static oshi.ffm.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

public final class InstalledAppsDataFFM {

    private static final Logger LOG = LoggerFactory.getLogger(InstalledAppsDataFFM.class);

    private static final long THIRTY_YEARS_IN_SECS = 30L * 365 * 24 * 60 * 60;

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
                                String name = registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "DisplayName", accessFlag));
                                if (name == null)
                                    continue;
                                String version = registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "DisplayVersion", accessFlag));
                                String publisher = registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "Publisher", accessFlag));
                                long installDate = registryValueToLong(
                                        getRegistryValueOrNull(rootKey, fullPath, "InstallDate", accessFlag));
                                String installLocation = registryValueToString(
                                        getRegistryValueOrNull(rootKey, fullPath, "InstallLocation", accessFlag));
                                String installSource = registryValueToString(
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

    private static String registryValueToString(Object registryValueOrNull) {
        if (registryValueOrNull instanceof Integer i) {
            return Integer.toString(i);
        }
        return (String) registryValueOrNull;
    }

    private static long registryValueToLong(Object registryValueOrNull) {
        if (registryValueOrNull == null) {
            return 0L;
        }
        long currentTimeSecs = System.currentTimeMillis() / 1000L;
        long minSaneTimestamp = currentTimeSecs - THIRTY_YEARS_IN_SECS;

        if (registryValueOrNull instanceof Integer i) {
            if (i > minSaneTimestamp && i < currentTimeSecs) {
                return i * 1000L;
            }
            return i;
        } else if (registryValueOrNull instanceof String s) {
            String dateStr = s.trim();
            long epoch = ParseUtil.parseDateToEpoch(dateStr, "yyyyMMdd");
            if (epoch == 0) {
                epoch = ParseUtil.parseDateToEpoch(dateStr, "MM/dd/yyyy");
            }
            return epoch;
        }
        return 0L;
    }

    private static Object getRegistryValueOrNull(MemorySegment rootKey, String path, String key, int accessFlag)
            throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment phkKey = arena.allocate(ADDRESS);
            int rc = RegOpenKeyEx(rootKey, toWideString(arena, path), 0, KEY_READ | accessFlag, phkKey);
            if (rc != ERROR_SUCCESS) {
                throw new Win32Exception(rc);
            }

            MemorySegment hKey = phkKey.get(ADDRESS, 0);
            try {
                return registryGetValue(hKey, key);
            } finally {
                rc = RegCloseKey(hKey);
                if (rc != ERROR_SUCCESS) {
                    throw new Win32Exception(rc);
                }
            }
        } catch (Win32Exception e) {
            LOG.trace("Unable to access " + path + " with flag " + accessFlag + ": " + e.getMessage());
            return null;
        }
    }

    private static Object registryGetValue(MemorySegment hKey, String valueName) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, MemorySegment.NULL, lpcbData);
            if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER) {
                throw new Win32Exception(rc);
            }

            int type = lpType.get(JAVA_INT, 0);
            int size = lpcbData.get(JAVA_INT, 0);

            return switch (type) {
            case REG_SZ, REG_EXPAND_SZ -> registryGetString(hKey, valueName, size);
            case REG_DWORD -> registryGetDword(hKey, valueName);
            default -> {
                LOG.warn("Unsupported registry data type " + type + " for " + valueName);
                yield null;
            }
            };
        }
    }

    private static String registryGetString(MemorySegment hKey, String valueName, int size) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment data = arena.allocate(size + 2);
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, size);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, data, lpcbData);
            if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER) {
                throw new Win32Exception(rc);
            }
            return readWideString(data);
        }
    }

    private static int registryGetDword(MemorySegment hKey, String valueName) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pData = arena.allocate(JAVA_INT);
            MemorySegment lpType = arena.allocate(JAVA_INT);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, Integer.BYTES);

            int rc = RegQueryValueEx(hKey, toWideString(arena, valueName), 0, lpType, pData, lpcbData);
            if (rc != ERROR_SUCCESS && rc != ERROR_INSUFFICIENT_BUFFER) {
                throw new Win32Exception(rc);
            }
            return pData.get(JAVA_INT, 0);
        }
    }

}
