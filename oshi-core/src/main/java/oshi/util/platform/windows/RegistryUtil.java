/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.platform.win32.WinReg.HKEY;

import oshi.driver.common.windows.registry.RegistryValueUtil;

/**
 * Utility for reading values from the Windows Registry.
 */
public final class RegistryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RegistryUtil.class);

    private RegistryUtil() {
    }

    /**
     * Returns a registry value as a Long. (with access flag) Currently supports String and Integer
     *
     * @param root       the registry root
     * @param path       the registry path
     * @param key        the registry key
     * @param accessFlag the access flag
     * @return the registry value as a long
     */
    public static long getLongValue(HKEY root, String path, String key, int accessFlag) {
        Object val = getRegistryValueOrNull(root, path, key, accessFlag);
        return RegistryValueUtil.registryValueToLong(val);
    }

    /**
     * Gets a registry value or null if not found
     *
     * @param root       the registry root
     * @param path       the registry path
     * @param key        the registry key
     * @param accessFlag the access flag
     * @return the registry value or null
     */
    public static Object getRegistryValueOrNull(HKEY root, String path, String key, int accessFlag) {
        try {
            // registryGetValues opens the key with KEY_READ | accessFlag, so the WOW64 access flag (e.g.
            // KEY_WOW64_32KEY/KEY_WOW64_64KEY) is honored when reading the value. Reading the value separately via
            // registryGetValue(root, path, key) re-opened the key with the default view, ignoring accessFlag.
            Map<String, Object> values = Advapi32Util.registryGetValues(root, path, accessFlag);
            // A null key requests the (Default) value, which registryGetValues stores under the empty name.
            String valueName = key == null ? "" : key;
            Object value = values.get(valueName);
            if (value == null) {
                // Registry value names are case-insensitive; fall back to a case-insensitive match.
                for (Map.Entry<String, Object> entry : values.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(valueName)) {
                        return entry.getValue();
                    }
                }
            }
            return value;
        } catch (Win32Exception e) {
            LOG.trace("Unable to access {} with flag {}: {}", path, accessFlag, e.getMessage());
        }
        return null;
    }

    /**
     * Returns a registry value as a String. (with access flag) Currently supports String, Integer, and Binary
     *
     * @param root       the registry root
     * @param path       the registry path
     * @param key        the registry key
     * @param accessFlag the access flag
     * @return the registry value as a string
     */
    public static String getStringValue(WinReg.HKEY root, String path, String key, int accessFlag) {
        Object val = getRegistryValueOrNull(root, path, key, accessFlag);
        return RegistryValueUtil.registryValueToString(val);
    }

}
