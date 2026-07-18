/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.registry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ParseUtil;

/**
 * Utility for interpreting raw Windows Registry values. Shared by the JNA and FFM backends so the two cannot diverge in
 * how they coerce registry types to Java values.
 */
@ThreadSafe
public final class RegistryValueUtil {

    private static final long THIRTY_YEARS_IN_SECS = 30L * 365 * 24 * 60 * 60;

    private RegistryValueUtil() {
    }

    /**
     * Interprets a registry value as a String, handling the three types encountered in practice: {@code REG_SZ} /
     * {@code REG_EXPAND_SZ} (a {@link String}), {@code REG_DWORD} (an {@link Integer}), and {@code REG_BINARY} (a
     * {@code byte[]}).
     *
     * @param val the raw registry value, or {@code null}
     * @return the value as a String, or {@code null} if the value is {@code null} or an unsupported type
     */
    public static String registryValueToString(Object val) {
        // REG_SZ / REG_EXPAND_SZ
        if (val instanceof String) {
            return ((String) val).trim();
        }
        // REG_DWORD (unsigned 32-bit)
        if (val instanceof Integer) {
            return Integer.toUnsignedString((Integer) val);
        }
        // REG_BINARY
        if (val instanceof byte[]) {
            return ParseUtil.decodeBinaryToString((byte[]) val);
        }
        return null;
    }

    /**
     * Interprets a registry value as a long. Supports {@link Integer} values (treating those in a plausible Unix
     * timestamp range as seconds and converting to milliseconds) and {@link String} dates ({@code yyyyMMdd} or
     * {@code MM/dd/yyyy}).
     *
     * @param val the raw registry value, or {@code null}
     * @return the value as a long, or {@code 0} if the value is {@code null} or an unsupported type
     */
    public static long registryValueToLong(Object val) {
        if (val == null) {
            return 0L;
        }
        // Calculate reasonable timestamp bounds (current time to 30 years ago)
        long currentTimeSecs = System.currentTimeMillis() / 1000L;
        long minSaneTimestamp = currentTimeSecs - THIRTY_YEARS_IN_SECS;
        if (val instanceof Integer) {
            // REG_DWORD is unsigned; interpret it as such so timestamps past 2038 (high bit set) are not treated as
            // negative.
            long value = Integer.toUnsignedLong((Integer) val);
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
}
