/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.gpu;

import java.util.List;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.ParseUtil;

/**
 * Shared utility methods for DXGI adapter matching and related parsing. Contains no native code.
 */
@ThreadSafe
public final class DxgiUtil {

    private DxgiUtil() {
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
     * @param adapters    list of adapters
     * @param vendorId    PCI vendor ID parsed from the registry key (0 if unknown)
     * @param deviceId    PCI device ID parsed from the registry key (0 if unknown)
     * @param adapterName adapter name from the registry {@code DriverDesc} value
     * @return best-matching adapter, or {@code null}
     */
    public static DxgiAdapterInfo findMatch(List<DxgiAdapterInfo> adapters, int vendorId, int deviceId,
            String adapterName) {
        if (vendorId != 0 && deviceId != 0) {
            for (DxgiAdapterInfo a : adapters) {
                if (a.getVendorId() == vendorId && a.getDeviceId() == deviceId) {
                    return a;
                }
            }
        }
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

    /**
     * Builds the PDH LUID instance prefix for the given DXGI adapter. The prefix has the form
     * {@code luid_0xHHHHHHHH_0xLLLLLLLL_phys_0}.
     *
     * @param adapter the DXGI adapter info containing the LUID
     * @return PDH LUID instance prefix string, or empty string if the LUID is zero
     */
    public static String buildLuidPrefix(DxgiAdapterInfo adapter) {
        int low = adapter.getLuidLowPart();
        int high = adapter.getLuidHighPart();
        if (low == 0 && high == 0) {
            return "";
        }
        return String.format(Locale.ROOT, "luid_0x%08x_0x%08x_phys_0", high, low);
    }

    /**
     * Parses the PCI bus number from a Windows registry {@code LocationInformation} string of the form
     * {@code "PCI bus N, device N, function N"}.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI bus number, or -1 if not parseable
     */
    public static int parsePciBusNumber(String locationInfo) {
        if (locationInfo == null || locationInfo.isEmpty()) {
            return -1;
        }
        String lower = locationInfo.toLowerCase(Locale.ROOT);
        int busIdx = lower.indexOf("pci bus ");
        if (busIdx < 0) {
            return -1;
        }
        int start = busIdx + 8;
        int end = lower.indexOf(',', start);
        String numStr = end > start ? locationInfo.substring(start, end).trim() : locationInfo.substring(start).trim();
        return ParseUtil.parseIntOrDefault(numStr, -1);
    }

    /**
     * Parses the PCI device number from a Windows registry {@code LocationInformation} string.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI device number, or -1 if not parseable
     */
    public static int parsePciDevice(String locationInfo) {
        if (locationInfo == null || locationInfo.isEmpty()) {
            return -1;
        }
        String lower = locationInfo.toLowerCase(Locale.ROOT);
        int devIdx = lower.indexOf("device ");
        if (devIdx < 0) {
            return -1;
        }
        int start = devIdx + 7;
        int end = lower.indexOf(',', start);
        String numStr = end > start ? locationInfo.substring(start, end).trim() : locationInfo.substring(start).trim();
        return ParseUtil.parseIntOrDefault(numStr, -1);
    }

    /**
     * Parses the PCI function number from a Windows registry {@code LocationInformation} string.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI function number, or -1 if not parseable
     */
    public static int parsePciFunction(String locationInfo) {
        if (locationInfo == null || locationInfo.isEmpty()) {
            return -1;
        }
        String lower = locationInfo.toLowerCase(Locale.ROOT);
        int fnIdx = lower.indexOf("function ");
        if (fnIdx < 0) {
            return -1;
        }
        int start = fnIdx + 9;
        int end = lower.indexOf(',', start);
        String numStr = end > start ? locationInfo.substring(start, end).trim() : locationInfo.substring(start).trim();
        return ParseUtil.parseIntOrDefault(numStr, -1);
    }

    /**
     * Builds a PCI bus ID string in {@code "0000:BB:DD.F"} format from a Windows registry {@code LocationInformation}
     * string.
     *
     * @param locationInfo the LocationInformation registry value
     * @return PCI bus ID string, or empty string if not parseable
     */
    public static String buildPciBusId(String locationInfo) {
        int bus = parsePciBusNumber(locationInfo);
        int device = parsePciDevice(locationInfo);
        int function = parsePciFunction(locationInfo);
        if (bus < 0 || device < 0 || function < 0) {
            return "";
        }
        return String.format(Locale.ROOT, "0000:%02x:%02x.%x", bus, device, function);
    }
}
