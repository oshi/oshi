/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinReg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.ParseUtil;

import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.platform.win32.WinReg.HKEYByReference;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import static com.sun.jna.platform.win32.WinError.ERROR_SUCCESS;
import static com.sun.jna.platform.win32.WinNT.KEY_READ;

public final class RegistryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(RegistryUtil.class);

    private static final long THIRTY_YEARS_IN_SECS = 30L * 365 * 24 * 60 * 60;

    private static final Advapi32 ADV = Advapi32.INSTANCE;

    private RegistryUtil() {
    }

    /**
     * Returns a registry value as a Long. (without access flag) Currently supports String and Integer
     */
    public static long getLongValue(HKEY root, String path, String key) {
        try {
            Object val = Advapi32Util.registryGetValue(root, path, key);
            return registryValueToLong(val);
        } catch (Win32Exception e) {
            LOG.trace("Unable to access " + path + ": " + e.getMessage());
        }
        return 0L;
    }

    /**
     * Returns a registry value as a String. (with access flag) Currently supports String and Integer
     */
    public static long getLongValue(HKEY root, String path, String key, int accessFlag) {
        Object val = getRegistryValueOrNull(root, path, key, accessFlag);
        return registryValueToLong(val);
    }

    public static Object getRegistryValueOrNull(HKEY root, String path, String key, int accessFlag) {
        HKEY hKey = null;
        try {
            hKey = getRegistryHKey(root, path, accessFlag);
            Object value = Advapi32Util.registryGetValue(root, path, key);
            if ((value instanceof Integer) || (value instanceof String && !((String) value).trim().isEmpty())
                    || value instanceof byte[]) {
                return value;
            }
        } catch (Win32Exception e) {
            LOG.trace("Unable to access " + path + " with flag " + accessFlag + ": " + e.getMessage());
        } finally {
            if (hKey != null) {
                int rc = ADV.RegCloseKey(hKey);
                if (rc != ERROR_SUCCESS) {
                    throw new Win32Exception(rc);
                }
            }
        }
        return null;
    }

    private static HKEY getRegistryHKey(HKEY rootKey, String path, int accessFlag) {
        HKEYByReference phkKey = new HKEYByReference();
        int rc = ADV.RegOpenKeyEx(rootKey, path, 0, KEY_READ | accessFlag, phkKey);
        if (rc != ERROR_SUCCESS) {
            throw new Win32Exception(rc);
        }
        return phkKey.getValue();
    }

    private static long registryValueToLong(Object val) {
        if (val == null) {
            return 0L;
        }

        // Calculate reasonable timestamp bounds (current time to 30 years ago)
        long currentTimeSecs = System.currentTimeMillis() / 1000L;
        long minSaneTimestamp = currentTimeSecs - THIRTY_YEARS_IN_SECS;
        if (val instanceof Integer) {
            int value = (Integer) val;
            if (value > minSaneTimestamp && value < currentTimeSecs) {
                return value * 1000L;
            }
            return value;
        } else if (val instanceof String) {
            String dateStr = ((String) val).trim();
            // Try yyyyMMdd first
            long epoch = ParseUtil.parseDateToEpoch(dateStr, "yyyyMMdd");
            if (epoch == 0) {
                // If that fails, try MM/dd/yyyy
                epoch = ParseUtil.parseDateToEpoch(dateStr, "MM/dd/yyyy");
            }
            return epoch;
        }
        return 0L;
    }

    /**
     * Returns a registry value as a String. (without access flag) Currently supports String and Binary
     */
    public static String getStringValue(WinReg.HKEY root, String path, String key) throws Win32Exception {
        try {
            Object val = Advapi32Util.registryGetValue(root, path, key);
            return registryValueToString(val);
        } catch (Win32Exception e) {
            LOG.trace("Unable to access " + path + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns a registry value as a String. (with access flag) Currently supports String and Binary
     */
    public static String getStringValue(WinReg.HKEY root, String path, String key, int accessFlag)
            throws Win32Exception {
        Object val = getRegistryValueOrNull(root, path, key, accessFlag);
        return registryValueToString(val);
    }

    /**
     * Decodes registry value to String using multiple fallback encodings.
     */
    private static String registryValueToString(Object val) {
        if (val == null)
            return null;

        // Already a string (REG_SZ or REG_EXPAND_SZ)
        if (val instanceof String)
            return ((String) val).trim();

        // handle binary (REG_BINARY)
        if (val instanceof byte[]) {
            return decodeBinaryToString((byte[]) val);
        }

        return null;
    }

    /**
     * Attempts to decode REG_BINARY into a string, trying UTF-16LE → UTF-8 → Windows-1252 → Hex fallback.
     */
    private static String decodeBinaryToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return null;

        String[] encodings = { "UTF-16LE", "UTF-8", "Windows-1252" };

        for (String enc : encodings) {
            try {
                String decoded = new String(bytes, enc).trim();
                if (!decoded.isEmpty()) {
                    return decoded;
                }
            } catch (UnsupportedEncodingException e) {
            }
        }

        // fall back to Hex
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT,"%02X ", b));
        }

        return sb.length() == 0 ? null : sb.toString();
    }
}
