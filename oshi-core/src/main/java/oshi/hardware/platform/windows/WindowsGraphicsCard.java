/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;
import com.sun.jna.platform.win32.VersionHelpers;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinReg;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.wmi.Win32VideoController;
import oshi.driver.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.util.Constants;
import oshi.util.ParseUtil;
import oshi.util.Util;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Triplet;

/**
 * Graphics Card obtained from WMI
 */
@Immutable
final class WindowsGraphicsCard extends AbstractGraphicsCard {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGraphicsCard.class);

    private static final boolean IS_VISTA_OR_GREATER = VersionHelpers.IsWindowsVistaOrGreater();
    public static final String ADAPTER_STRING = "HardwareInformation.AdapterString";
    public static final String DRIVER_DESC = "DriverDesc";
    public static final String DRIVER_VERSION = "DriverVersion";
    public static final String VENDOR = "ProviderName";
    public static final String QW_MEMORY_SIZE = "HardwareInformation.qwMemorySize";
    public static final String MEMORY_SIZE = "HardwareInformation.MemorySize";
    public static final String DISPLAY_DEVICES_REGISTRY_PATH =
        "SYSTEM\\CurrentControlSet\\Control\\Class\\{4d36e968-e325-11ce-bfc1-08002be10318}\\";

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

                String name = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, DRIVER_DESC);
                String deviceId = null; // Note: not found proper key in registry
                String vendor = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, VENDOR);
                String versionInfo = Advapi32Util.registryGetStringValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, DRIVER_VERSION);
                long vram = 0L;

                if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, QW_MEMORY_SIZE)) {
                    vram = Advapi32Util.registryGetLongValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, QW_MEMORY_SIZE);
                } else if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, fullKey, MEMORY_SIZE)) {
                    Object o = Advapi32Util.registryGetValue(WinReg.HKEY_LOCAL_MACHINE, fullKey, MEMORY_SIZE);
                    if (o instanceof Long || o instanceof Integer) {
                        vram = (long) o;
                    } else if (o instanceof byte[]) {
                        vram = memSizeBinaryToLong((byte[])o);
                    }
                }

                cardList.add(new WindowsGraphicsCard(
                    Util.isBlank(name) ? Constants.UNKNOWN : name,
                    Util.isBlank(deviceId) ? Constants.UNKNOWN : deviceId,
                    Util.isBlank(vendor) ? Constants.UNKNOWN : vendor,
                    Util.isBlank(versionInfo) ? Constants.UNKNOWN : versionInfo,
                    vram));
            } catch (Win32Exception e) {
                if (e.getErrorCode() != WinError.ERROR_ACCESS_DENIED) {
                    // Ignore access denied errors, re-throw others
                    throw e;
                }
            }
        }

        if (cardList.isEmpty()) {
            return getGraphicsCardsFromWmi();
        }
        return cardList;
    }

    // Note: not found spec, working for my Intel UHD embedded gpu
    private static long memSizeBinaryToLong(byte[] bytes) {
        long result = 0;

        if (bytes.length > 8) {
            LOG.debug("byte[] to long: Oops! bytes length > 8");
            return 0;
        }

        for (byte i = 0; i < bytes.length; ++i) {
            result += (long) (bytes[i] & 255) << 8 * i;
        }

        return result;
    }

    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link oshi.hardware.platform.windows.WindowsGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCardsFromWmi() {
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
                long vram = WmiUtil.getUint32asLong(cards, VideoControllerProperty.ADAPTERRAM, index);
                cardList.add(new WindowsGraphicsCard(Util.isBlank(name) ? Constants.UNKNOWN : name, deviceId,
                        Util.isBlank(vendor) ? Constants.UNKNOWN : vendor, versionInfo, vram));
            }
        }
        return cardList;
    }
}
