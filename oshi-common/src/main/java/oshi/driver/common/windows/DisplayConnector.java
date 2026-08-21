/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.Util;

/**
 * Shared logic for mapping a Windows display to the physical connector it is attached to, using the Connecting and
 * Configuring Displays (CCD) APIs in {@code user32.dll}.
 * <p>
 * The struct sizes and field offsets the two native backends read are declared here as constants so the JNA and FFM
 * implementations cannot drift apart, and the pure parsing (connector naming, device-path normalization) is shared and
 * unit-tested. The native calls themselves - {@code GetDisplayConfigBufferSizes}, {@code QueryDisplayConfig}, and
 * {@code DisplayConfigGetDeviceInfo} - are made by each backend.
 * <p>
 * A {@code LUID} is two {@code DWORD}s, so it is only 4-byte aligned and the {@code adapterId} fields below can land on
 * an offset that is not a multiple of 8. An 8-byte read there must therefore be unaligned, which the FFM backend
 * enforces at runtime.
 */
@ThreadSafe
public final class DisplayConnector {

    private DisplayConnector() {
    }

    /** {@code QDC_ONLY_ACTIVE_PATHS} flag for {@code GetDisplayConfigBufferSizes}/{@code QueryDisplayConfig}. */
    public static final int QDC_ONLY_ACTIVE_PATHS = 0x00000002;

    /** Size in bytes of a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_INFO_SIZE = 72;
    /** Size in bytes of a {@code DISPLAYCONFIG_MODE_INFO} (allocated but never read). */
    public static final int MODE_INFO_SIZE = 64;
    /** Offset of {@code targetInfo.adapterId} (an 8-byte {@code LUID}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_TARGET_ADAPTER_ID_OFFSET = 20;
    /** Offset of {@code targetInfo.id} (a {@code UINT32}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_TARGET_ID_OFFSET = 28;
    /** Offset of the path {@code flags} (a {@code UINT32}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_FLAGS_OFFSET = 68;
    /** {@code DISPLAYCONFIG_PATH_ACTIVE} bit within the path {@code flags}. */
    public static final int PATH_ACTIVE = 0x00000001;

    /** Size in bytes of a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}. */
    public static final int TARGET_DEVICE_NAME_SIZE = 420;
    /** {@code DISPLAYCONFIG_DEVICE_INFO_GET_TARGET_NAME} request type. */
    public static final int DEVICE_INFO_GET_TARGET_NAME = 2;
    /** Offset of {@code header.size} within a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}. */
    public static final int TDN_HEADER_SIZE_OFFSET = 4;
    /**
     * Offset of {@code header.adapterId} (an 8-byte {@code LUID}) within a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}.
     */
    public static final int TDN_HEADER_ADAPTER_ID_OFFSET = 8;
    /** Offset of {@code header.id} (a {@code UINT32}) within a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}. */
    public static final int TDN_HEADER_ID_OFFSET = 16;
    /** Offset of {@code outputTechnology} within a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}. */
    public static final int TDN_OUTPUT_TECHNOLOGY_OFFSET = 24;
    /** Offset of {@code connectorInstance} within a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}. */
    public static final int TDN_CONNECTOR_INSTANCE_OFFSET = 32;
    /** Offset of {@code monitorDevicePath} (a {@code WCHAR[128]}) within a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}. */
    public static final int TDN_MONITOR_DEVICE_PATH_OFFSET = 164;

    // DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY values (shared with the D3DKMDT_VIDEO_OUTPUT_TECHNOLOGY enum).
    private static final int VOT_HD15 = 0;
    private static final int VOT_SVIDEO = 1;
    private static final int VOT_COMPOSITE_VIDEO = 2;
    private static final int VOT_COMPONENT_VIDEO = 3;
    private static final int VOT_DVI = 4;
    private static final int VOT_HDMI = 5;
    private static final int VOT_LVDS = 6;
    private static final int VOT_SDI = 9;
    private static final int VOT_DISPLAYPORT_EXTERNAL = 10;
    private static final int VOT_DISPLAYPORT_EMBEDDED = 11;
    private static final int VOT_UDI_EXTERNAL = 12;
    private static final int VOT_UDI_EMBEDDED = 13;
    private static final int VOT_SDTVDONGLE = 14;
    private static final int VOT_MIRACAST = 15;
    private static final int VOT_INTERNAL = 0x80000000;

    /**
     * Names the connector a display is attached through, from a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}'s
     * {@code outputTechnology} and {@code connectorInstance}. The {@code connectorInstance} is Windows' own
     * disambiguator, zero when the adapter has a single connector of that type and one-based when it has several, so it
     * is appended verbatim only when non-zero (e.g. {@code HDMI}, {@code DisplayPort-1}, {@code DisplayPort-2}).
     *
     * @param outputTechnology  the {@code DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY} value
     * @param connectorInstance the connector instance number
     * @return a connector name such as {@code HDMI} or {@code DisplayPort-1}
     */
    public static String connectorName(int outputTechnology, int connectorInstance) {
        String base = technologyName(outputTechnology);
        return connectorInstance > 0 ? base + "-" + connectorInstance : base;
    }

    private static String technologyName(int outputTechnology) {
        switch (outputTechnology) {
            case VOT_HD15:
                return "VGA";
            case VOT_SVIDEO:
                return "S-Video";
            case VOT_COMPOSITE_VIDEO:
                return "Composite";
            case VOT_COMPONENT_VIDEO:
                return "Component";
            case VOT_DVI:
                return "DVI";
            case VOT_HDMI:
                return "HDMI";
            case VOT_LVDS:
                return "LVDS";
            case VOT_SDI:
                return "SDI";
            case VOT_DISPLAYPORT_EXTERNAL:
                return "DisplayPort";
            case VOT_DISPLAYPORT_EMBEDDED:
                return "eDP";
            case VOT_UDI_EXTERNAL:
            case VOT_UDI_EMBEDDED:
                return "UDI";
            case VOT_SDTVDONGLE:
                return "SDTV";
            case VOT_MIRACAST:
                return "Miracast";
            case VOT_INTERNAL:
                return "Internal";
            default:
                return "Other";
        }
    }

    /**
     * Normalizes a monitor device interface path for case-insensitive matching. The path returned by
     * {@code SetupDiGetDeviceInterfaceDetail} and the {@code monitorDevicePath} returned by
     * {@code DisplayConfigGetDeviceInfo} are the same string but may differ in case.
     *
     * @param devicePath the device interface path, or {@code null}
     * @return the lower-cased path, or {@link Constants#UNKNOWN} if the input is null or empty
     */
    public static String normalizePath(String devicePath) {
        if (Util.isBlank(devicePath)) {
            return Constants.UNKNOWN;
        }
        return devicePath.toLowerCase(Locale.ROOT);
    }
}
