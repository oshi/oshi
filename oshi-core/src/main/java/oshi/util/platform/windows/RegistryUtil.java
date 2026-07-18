/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static com.sun.jna.platform.win32.WinError.ERROR_SUCCESS;
import static com.sun.jna.platform.win32.WinNT.KEY_READ;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32;
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

    private static final Advapi32 ADV = Advapi32.INSTANCE;

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
        HKEY hKey = null;
        try {
            hKey = Advapi32Util.registryGetKey(root, path, KEY_READ | accessFlag).getValue();
            Object value = Advapi32Util.registryGetValue(root, path, key);
            return Objects.isNull(value) ? null : value;
        } catch (Win32Exception e) {
            LOG.trace("Unable to access {} with flag {}: {}", path, accessFlag, e.getMessage());
        } finally {
            if (hKey != null) {
                int rc = ADV.RegCloseKey(hKey);
                if (rc != ERROR_SUCCESS) {
                    LOG.trace("Unable to close registry key {}: {}", path, rc);
                }
            }
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
