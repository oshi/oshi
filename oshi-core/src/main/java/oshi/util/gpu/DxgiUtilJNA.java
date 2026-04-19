/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.gpu;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.gpu.DxgiAdapterInfo;
import oshi.driver.common.windows.gpu.DxgiUtil;
import oshi.jna.platform.windows.Dxgi;

/**
 * Utility methods for DXGI adapter enumeration and matching on Windows.
 */
@ThreadSafe
public final class DxgiUtilJNA {

    private DxgiUtilJNA() {
    }

    /**
     * Enumerates all DXGI display adapters and returns their identity and dedicated video memory.
     *
     * @return list of {@link DxgiAdapterInfo}, one per adapter; empty if DXGI is unavailable
     */
    public static List<DxgiAdapterInfo> queryAdapters() {
        return Dxgi.queryAdapters();
    }

    /**
     * Finds the best-matching DXGI adapter for a given vendor ID, device ID, and adapter name.
     *
     * @param adapters    list from {@link #queryAdapters()}
     * @param vendorId    PCI vendor ID parsed from the registry key (0 if unknown)
     * @param deviceId    PCI device ID parsed from the registry key (0 if unknown)
     * @param adapterName adapter name from the registry {@code DriverDesc} value
     * @return best-matching adapter, or {@code null}
     */
    public static DxgiAdapterInfo findMatch(List<DxgiAdapterInfo> adapters, int vendorId, int deviceId,
            String adapterName) {
        return DxgiUtil.findMatch(adapters, vendorId, deviceId, adapterName);
    }

    /**
     * Converts a registry value to a VRAM size in bytes.
     *
     * @param value the registry value object
     * @return the VRAM size in bytes, or 0 if the value type is unrecognised
     */
    public static long registryValueToVram(Object value) {
        return DxgiUtil.registryValueToVram(value);
    }

    /**
     * Normalizes an adapter name for fuzzy matching.
     *
     * @param name the raw adapter name, may be {@code null}
     * @return normalized name, never {@code null}
     */
    public static String normalizeName(String name) {
        return DxgiUtil.normalizeName(name);
    }
}
