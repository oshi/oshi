/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows;

import java.util.Locale;
import java.util.Optional;
import java.util.function.LongToIntFunction;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.DisplayMode;
import oshi.hardware.DisplayModeImpl;
import oshi.util.Constants;
import oshi.util.Util;

/**
 * Shared logic for mapping a Windows display to the physical connector it is attached to, using the Connecting and
 * Configuring Displays (CCD) APIs in {@code user32.dll}.
 * <p>
 * The struct sizes and field offsets the two native backends read are declared here as constants so the JNA and FFM
 * implementations cannot drift apart, and the pure parsing (connector naming, built-in classification, the current
 * mode, device-path normalization) is shared and unit-tested. The native calls themselves -
 * {@code GetDisplayConfigBufferSizes}, {@code QueryDisplayConfig}, and {@code DisplayConfigGetDeviceInfo} - are made by
 * each backend.
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
    /** Size in bytes of a {@code DISPLAYCONFIG_MODE_INFO}. */
    public static final int MODE_INFO_SIZE = 64;
    /** Offset of {@code sourceInfo.modeInfoIdx} (a {@code UINT32}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_SOURCE_MODE_IDX_OFFSET = 12;
    /** Offset of {@code targetInfo.adapterId} (an 8-byte {@code LUID}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_TARGET_ADAPTER_ID_OFFSET = 20;
    /** Offset of {@code targetInfo.id} (a {@code UINT32}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_TARGET_ID_OFFSET = 28;
    /**
     * Offset of {@code targetInfo.rotation} (a {@code DISPLAYCONFIG_ROTATION}) within a
     * {@code DISPLAYCONFIG_PATH_INFO}.
     */
    public static final int PATH_TARGET_ROTATION_OFFSET = 40;
    /** Offset of {@code targetInfo.refreshRate.Numerator} within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_TARGET_REFRESH_NUMERATOR_OFFSET = 48;
    /** Offset of {@code targetInfo.refreshRate.Denominator} within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_TARGET_REFRESH_DENOMINATOR_OFFSET = 52;
    /** Offset of the path {@code flags} (a {@code UINT32}) within a {@code DISPLAYCONFIG_PATH_INFO}. */
    public static final int PATH_FLAGS_OFFSET = 68;
    /** {@code DISPLAYCONFIG_PATH_ACTIVE} bit within the path {@code flags}. */
    public static final int PATH_ACTIVE = 0x00000001;

    // DISPLAYCONFIG_MODE_INFO: infoType@0, id@4, adapterId@8, then a union at 16 which, for a source mode, is a
    // DISPLAYCONFIG_SOURCE_MODE of width, height, pixelFormat, and a POINTL position.
    private static final int MODE_INFO_TYPE_OFFSET = 0;
    private static final int MODE_INFO_TYPE_SOURCE = 1;
    private static final int SOURCE_MODE_WIDTH_OFFSET = 16;
    private static final int SOURCE_MODE_HEIGHT_OFFSET = 20;
    private static final int SOURCE_MODE_POSITION_X_OFFSET = 28;
    private static final int SOURCE_MODE_POSITION_Y_OFFSET = 32;

    // DISPLAYCONFIG_ROTATION values, each a clockwise rotation of the desktop.
    private static final int ROTATION_IDENTITY = 1;
    private static final int ROTATION_ROTATE90 = 2;
    private static final int ROTATION_ROTATE180 = 3;
    private static final int ROTATION_ROTATE270 = 4;

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
     * Tests whether an output technology connects a display built into the device.
     *
     * @param outputTechnology the {@code DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY} value
     * @return true for LVDS, embedded DisplayPort, embedded UDI, and internal connections
     */
    public static boolean isBuiltIn(int outputTechnology) {
        switch (outputTechnology) {
            case VOT_LVDS:
            case VOT_DISPLAYPORT_EMBEDDED:
            case VOT_UDI_EMBEDDED:
            case VOT_INTERNAL:
                return true;
            default:
                return false;
        }
    }

    /**
     * Reads the current mode of one active path from the buffers {@code QueryDisplayConfig} filled. The desktop
     * position and size come from the source mode the path indexes into the mode array, and the rotation and refresh
     * rate from the path's target. Windows reports the desktop in physical pixels, so the logical and pixel sizes are
     * equal.
     *
     * @param path      reads a {@code UINT32} at an offset within the path's {@code DISPLAYCONFIG_PATH_INFO}
     * @param modes     reads a {@code UINT32} at an offset within the {@code DISPLAYCONFIG_MODE_INFO} array
     * @param modeCount the number of entries in the mode array
     * @return the path's current mode, or {@code null} if it does not index a source mode
     */
    public static @Nullable DisplayMode readMode(LongToIntFunction path, LongToIntFunction modes, int modeCount) {
        // A flat UINT32 because QueryDisplayConfig is called without QDC_VIRTUAL_MODE_AWARE; with that flag the field
        // becomes a union of cloneGroupId and sourceModeInfoIdx bitfields
        int index = path.applyAsInt(PATH_SOURCE_MODE_IDX_OFFSET);
        // DISPLAYCONFIG_PATH_MODE_IDX_INVALID is 0xffffffff, negative as an int
        if (index < 0 || index >= modeCount) {
            return null;
        }
        long base = (long) index * MODE_INFO_SIZE;
        if (modes.applyAsInt(base + MODE_INFO_TYPE_OFFSET) != MODE_INFO_TYPE_SOURCE) {
            return null;
        }
        int width = modes.applyAsInt(base + SOURCE_MODE_WIDTH_OFFSET);
        int height = modes.applyAsInt(base + SOURCE_MODE_HEIGHT_OFFSET);
        if (width <= 0 || height <= 0) {
            return null;
        }
        int x = modes.applyAsInt(base + SOURCE_MODE_POSITION_X_OFFSET);
        int y = modes.applyAsInt(base + SOURCE_MODE_POSITION_Y_OFFSET);
        int rotation = rotationDegrees(path.applyAsInt(PATH_TARGET_ROTATION_OFFSET));
        double refreshRate = refreshRate(path.applyAsInt(PATH_TARGET_REFRESH_NUMERATOR_OFFSET),
                path.applyAsInt(PATH_TARGET_REFRESH_DENOMINATOR_OFFSET));
        return new DisplayModeImpl(x, y, width, height, width, height, refreshRate, rotation);
    }

    /**
     * Converts a {@code DISPLAYCONFIG_ROTATION} to degrees clockwise.
     *
     * @param rotation the {@code DISPLAYCONFIG_ROTATION} value
     * @return 0, 90, 180 or 270; 0 for an unrecognized value
     */
    static int rotationDegrees(int rotation) {
        switch (rotation) {
            case ROTATION_ROTATE90:
                return 90;
            case ROTATION_ROTATE180:
                return 180;
            case ROTATION_ROTATE270:
                return 270;
            case ROTATION_IDENTITY:
            default:
                return 0;
        }
    }

    /**
     * Converts a {@code DISPLAYCONFIG_RATIONAL} refresh rate to hertz. The fields are unsigned.
     *
     * @param numerator   the numerator
     * @param denominator the denominator
     * @return the refresh rate in hertz, or 0 if the denominator is 0
     */
    static double refreshRate(int numerator, int denominator) {
        if (denominator == 0) {
            return 0d;
        }
        return (double) Integer.toUnsignedLong(numerator) / Integer.toUnsignedLong(denominator);
    }

    /**
     * A display's connector as the CCD API describes it, and the mode it is currently driven in.
     */
    @Immutable
    public static final class Connector {

        private final String name;
        private final boolean builtIn;
        private final @Nullable DisplayMode mode;

        /**
         * Describes a connector from a {@code DISPLAYCONFIG_TARGET_DEVICE_NAME}'s fields and the path's mode.
         *
         * @param outputTechnology  the {@code DISPLAYCONFIG_VIDEO_OUTPUT_TECHNOLOGY} value
         * @param connectorInstance the connector instance number
         * @param mode              the path's current mode, as read by {@link #readMode}, or {@code null} if unknown
         */
        public Connector(int outputTechnology, int connectorInstance, @Nullable DisplayMode mode) {
            this.name = connectorName(outputTechnology, connectorInstance);
            this.builtIn = DisplayConnector.isBuiltIn(outputTechnology);
            this.mode = mode;
        }

        /**
         * The connector name, as named by {@link DisplayConnector#connectorName(int, int)}.
         *
         * @return the connector name, e.g. {@code HDMI} or {@code DisplayPort-1}
         */
        public String getName() {
            return name;
        }

        /**
         * Whether the connector attaches a display built into the device.
         *
         * @return true if built in
         */
        public boolean isBuiltIn() {
            return builtIn;
        }

        /**
         * The mode the display on this connector is currently driven in.
         *
         * @return the current mode, or empty if unknown
         */
        public Optional<DisplayMode> getMode() {
            return Optional.ofNullable(mode);
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
