/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import java.util.List;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.DxgiAdapterInfo;
import oshi.jna.platform.windows.Dxgi;

/**
 * Utility methods for DXGI adapter enumeration and matching on Windows.
 */
@ThreadSafe
public final class DxgiUtilJNA {

    private DxgiUtilJNA() {
    }

    /**
     * Enumerates all DXGI display adapters and returns their identity and dedicated video memory. Delegates to
     * {@link Dxgi#queryAdapters()}.
     *
     * @return list of {@link DxgiAdapterInfo}, one per adapter; empty if DXGI is unavailable
     */
    public static List<DxgiAdapterInfo> queryAdapters() {
        return Dxgi.queryAdapters();
    }

    /**
     * Finds the best-matching DXGI adapter for a given vendor ID, device ID, and adapter name.
     *
     * <p>
     * Matching priority:
     * <ol>
     * <li>Vendor ID + Device ID (exact, both non-zero)</li>
     * <li>Normalized name match (case-insensitive, ignoring {@code (R)}, {@code (TM)}, extra spaces)</li>
     * </ol>
     *
     * <p>
     * If multiple adapters share the same vendor+device ID (e.g. multi-GPU), the first one is returned. If no confident
     * match is found, returns {@code null}.
     *
     * @param adapters    list from {@link #queryAdapters()}
     * @param vendorId    PCI vendor ID parsed from the registry key (0 if unknown)
     * @param deviceId    PCI device ID parsed from the registry key (0 if unknown)
     * @param adapterName adapter name from the registry {@code DriverDesc} value
     * @return best-matching adapter, or {@code null}
     */
    public static DxgiAdapterInfo findMatch(List<DxgiAdapterInfo> adapters, int vendorId, int deviceId,
            String adapterName) {
        // Priority 1: vendor + device ID
        if (vendorId != 0 && deviceId != 0) {
            for (DxgiAdapterInfo a : adapters) {
                if (a.getVendorId() == vendorId && a.getDeviceId() == deviceId) {
                    return a;
                }
            }
        }
        // Priority 2: normalized name
        if (adapterName != null && !adapterName.isEmpty()) {
            String norm = normalizeName(adapterName);
            for (DxgiAdapterInfo a : adapters) {
                if (normalizeName(a.getDescription()).equals(norm)) {
                    return a;
                }
            }
        }
        return null;
    }

    /**
     * Converts a registry value (REG_QWORD as Long, REG_DWORD as Integer, or REG_BINARY as byte[]) to a VRAM size in
     * bytes. REG_BINARY is interpreted as little-endian.
     *
     * @param value the registry value object
     * @return the VRAM size in bytes, or 0 if the value type is unrecognised
     */
    public static long registryValueToVram(Object value) {
        if (value instanceof Long) {
            return (long) value;
        } else if (value instanceof Integer) {
            return Integer.toUnsignedLong((int) value);
        } else if (value instanceof byte[]) {
            byte[] bytes = (byte[]) value;
            long total = 0L;
            int size = Math.min(bytes.length, 8);
            for (int i = 0; i < size; i++) {
                total = total << 8 | bytes[size - i - 1] & 0xff;
            }
            return total;
        }
        return 0L;
    }

    /**
     * Normalizes an adapter name for fuzzy matching: lower-case, strips {@code (R)}/{@code (TM)}, collapses whitespace.
     *
     * @param name the raw adapter name, may be {@code null}
     * @return normalized name, never {@code null}
     */
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.toLowerCase(Locale.ROOT).replace("(r)", "").replace("(tm)", "").replaceAll("\\s+", " ").trim();
    }
}
