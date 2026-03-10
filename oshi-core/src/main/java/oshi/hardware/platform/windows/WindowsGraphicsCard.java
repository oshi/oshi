/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.DxgiAdapterInfo;
import oshi.driver.windows.wmi.Win32VideoController;
import oshi.driver.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.jna.platform.windows.WindowsDxgi;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.windows.RegistryUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Graphics Card obtained from the Windows registry, with VRAM sourced from DXGI.
 *
 * <p>
 * VRAM detection priority:
 * <ol>
 * <li>{@code DXGI_ADAPTER_DESC.DedicatedVideoMemory} — the authoritative Windows API value, not subject to the 2 GiB
 * cap that affects the 32-bit registry field.</li>
 * <li>{@code HardwareInformation.qwMemorySize} (64-bit registry value) — used when DXGI enumeration is unavailable or
 * no adapter match is found.</li>
 * </ol>
 *
 * <p>
 * {@code HardwareInformation.MemorySize} (32-bit) is intentionally not used: Windows writes the sentinel value
 * {@code 0x7FFFF000} (~2 GiB) into this field for GPUs with more than 2 GiB of dedicated VRAM, making it unreliable for
 * modern discrete GPUs.
 */
@Immutable
final class WindowsGraphicsCard extends AbstractGraphicsCard {

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();

    public static final String ADAPTER_STRING = "HardwareInformation.AdapterString";
    public static final String DRIVER_DESC = "DriverDesc";
    public static final String DRIVER_VERSION = "DriverVersion";
    public static final String VENDOR = "ProviderName";
    public static final String QW_MEMORY_SIZE = "HardwareInformation.qwMemorySize";
    public static final String MATCHING_DEVICE_ID = "MatchingDeviceId";
    public static final String DISPLAY_DEVICES_REGISTRY_PATH = "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\";

    /**
     * Constructor for WindowsGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    WindowsGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link oshi.hardware.platform.windows.WindowsGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<GraphicsCard> cardList = new ArrayList<>();

        // Query DXGI once for all adapters. Fails gracefully to empty list if unavailable.
        // Use a mutable copy so matched entries can be consumed, preventing the same
        // DxgiAdapterInfo from being assigned to two registry cards with identical PCI IDs
        // (e.g. identical multi-GPU configurations).
        List<DxgiAdapterInfo> dxgiAdapters = new ArrayList<>(WindowsDxgi.queryAdapters());

        int index = 1;
        String[] keys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, DISPLAY_DEVICES_REGISTRY_PATH);
        for (String key : keys) {
            if (!key.startsWith("0")) {
                continue;
            }

            try {
                String fullKey = DISPLAY_DEVICES_REGISTRY_PATH + key;
                if (!Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, ADAPTER_STRING)) {
                    continue;
                }

                String name = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, DRIVER_DESC);
                String deviceId = "VideoController" + index++;
                String vendor = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, VENDOR);
                String versionInfo = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, DRIVER_VERSION);

                // Parse PCI vendor/device IDs from MatchingDeviceId (e.g. "pci\ven_8086&dev_56a0&...")
                String matchingDeviceId = RegistryUtil.getStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey,
                        MATCHING_DEVICE_ID);
                Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(matchingDeviceId);
                int pciVendorId = pciIds == null ? 0 : pciIds.getA();
                int pciDeviceId = pciIds == null ? 0 : pciIds.getB();

                // Primary: DXGI DedicatedVideoMemory.
                // Track whether a DXGI match was found separately from the vram value, so that a
                // legitimate DedicatedVideoMemory == 0 (e.g. a software/render-only adapter) is
                // preserved and does not trigger the registry fallback.
                long vram = -1L;
                DxgiAdapterInfo dxgiMatch = WindowsDxgi.findMatch(dxgiAdapters, pciVendorId, pciDeviceId, name);
                if (dxgiMatch != null) {
                    vram = dxgiMatch.getDedicatedVideoMemory();
                    // Consume the matched entry so it cannot be assigned to a second registry card
                    // with the same PCI IDs (e.g. identical multi-GPU configurations).
                    dxgiAdapters.remove(dxgiMatch);
                }

                // Fallback: 64-bit registry value qwMemorySize, only when DXGI had no match.
                if (vram < 0 && Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, QW_MEMORY_SIZE)) {
                    Object regValue = Advapi32Util.registryGetValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, QW_MEMORY_SIZE);
                    vram = registryValueToVram(regValue);
                }

                // Normalise sentinel: if still unresolved report 0.
                if (vram < 0) {
                    vram = 0L;
                }

                // HardwareInformation.MemorySize (32-bit) is intentionally omitted: Windows caps
                // it at 0x7FFFF000 (~2 GiB) for GPUs with more VRAM, making it unreliable.

                cardList.add(new WindowsGraphicsCard(Util.isBlank(name) ? Constants.UNKNOWN : name,
                        Util.isBlank(deviceId) ? Constants.UNKNOWN : deviceId,
                        Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                        Util.isBlank(versionInfo) ? Constants.UNKNOWN : versionInfo, vram));
            } catch (Win32Exception e) {
                if (e.getErrorCode() != WinError.ERROR_ACCESS_DENIED) {
                    // Ignore access denied errors, re-throw others
                    throw e;
                }
            }
        }

        if (cardList.isEmpty()) {
            return getGraphicsCardsFromWmi(dxgiAdapters);
        }
        return cardList;
    }

    /**
     * Converts a registry value (REG_QWORD as Long, REG_DWORD as Integer, or REG_BINARY as byte[]) to a VRAM size in
     * bytes. REG_BINARY is interpreted as little-endian.
     *
     * @param value the registry value object
     * @return the VRAM size in bytes, or 0 if the value type is unrecognised
     */
    static long registryValueToVram(Object value) {
        return WindowsDxgi.registryValueToVram(value);
    }

    // fall back if something went wrong
    private static List<GraphicsCard> getGraphicsCardsFromWmi(List<DxgiAdapterInfo> dxgiAdapters) {
        List<GraphicsCard> cardList = new ArrayList<>();
        if (IS_VISTA_OR_GREATER) {
            WmiResult<VideoControllerProperty> cards = Win32VideoController.queryVideoController();
            for (int index = 0; index < cards.getResultCount(); index++) {
                String name = WmiUtil.getString(cards, VideoControllerProperty.NAME, index);
                Triplet<String, String, String> idPair = ParseUtil.parseDeviceIdToVendorProductSerial(
                        WmiUtil.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
                String deviceId = idPair == null ? Constants.UNKNOWN : idPair.getB();
                String vendor = WmiUtil.getString(cards, VideoControllerProperty.ADAPTERCOMPATIBILITY, index);
                if (idPair != null) {
                    if (Util.isBlank(vendor)) {
                        deviceId = idPair.getA();
                    } else {
                        vendor = vendor + " (" + idPair.getA() + ")";
                    }
                }
                String versionInfo = WmiUtil.getString(cards, VideoControllerProperty.DRIVERVERSION, index);
                if (!Util.isBlank(versionInfo)) {
                    versionInfo = "DriverVersion=" + versionInfo;
                } else {
                    versionInfo = Constants.UNKNOWN;
                }
                // Prefer DXGI DedicatedVideoMemory when a match can be found via the PCI IDs
                // extracted from PNPDEVICEID. Fall back to WMI AdapterRAM (32-bit capped) only
                // when no DXGI match is available.
                Pair<Integer, Integer> pciIds = ParseUtil.parseDeviceIdToVendorProductIds(
                        WmiUtil.getString(cards, VideoControllerProperty.PNPDEVICEID, index));
                int pciVendorId = pciIds == null ? 0 : pciIds.getA();
                int pciDeviceId = pciIds == null ? 0 : pciIds.getB();
                DxgiAdapterInfo dxgiMatch = WindowsDxgi.findMatch(dxgiAdapters, pciVendorId, pciDeviceId, name);
                long vram;
                if (dxgiMatch != null) {
                    vram = dxgiMatch.getDedicatedVideoMemory();
                    dxgiAdapters.remove(dxgiMatch);
                } else {
                    vram = WmiUtil.getUint32asLong(cards, VideoControllerProperty.ADAPTERRAM, index);
                }
                cardList.add(new WindowsGraphicsCard(Util.isBlank(name) ? Constants.UNKNOWN : name, deviceId,
                        Util.isBlank(vendor) ? Constants.UNKNOWN : vendor, versionInfo, vram));
            }
        }
        return cardList;
    }
}
