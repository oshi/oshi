/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.mac;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.DisplayMode;
import oshi.hardware.DisplayModeImpl;

/**
 * An active display as CoreGraphics reports it, and the logic that matches it to a display enumerated through IOKit.
 * <p>
 * IOKit enumerates displays with their EDID, and CoreGraphics enumerates them with their mode, but the two share no
 * identifier. CoreGraphics does report the three identity fields of the EDID - {@code CGDisplayVendorNumber},
 * {@code CGDisplayModelNumber} and {@code CGDisplaySerialNumber} - so a display is matched by comparing those with its
 * EDID. The built-in panel on Apple Silicon has no EDID and is matched by {@code CGDisplayIsBuiltin} instead.
 * <p>
 * Each native backend makes the CoreGraphics calls and builds these; the matching is shared.
 */
@Immutable
public final class CoreGraphicsDisplay {

    private final int vendor;
    private final int model;
    private final int serial;
    private final boolean builtIn;
    private final boolean main;
    private final @Nullable DisplayMode mode;

    /**
     * Constructor for CoreGraphicsDisplay.
     *
     * @param vendor  the value of {@code CGDisplayVendorNumber}
     * @param model   the value of {@code CGDisplayModelNumber}
     * @param serial  the value of {@code CGDisplaySerialNumber}
     * @param builtIn the value of {@code CGDisplayIsBuiltin}
     * @param main    the value of {@code CGDisplayIsMain}
     * @param mode    the display's current mode, as built by {@link #toMode}, or {@code null} if it could not be read
     */
    public CoreGraphicsDisplay(int vendor, int model, int serial, boolean builtIn, boolean main,
            @Nullable DisplayMode mode) {
        this.vendor = vendor;
        this.model = model;
        this.serial = serial;
        this.builtIn = builtIn;
        this.main = main;
        this.mode = mode;
    }

    /**
     * Whether CoreGraphics classifies this display as built in.
     *
     * @return true if built in
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    /**
     * Whether CoreGraphics reports this display as the main display, the one holding the menu bar and the desktop
     * origin.
     *
     * @return true if the main display
     */
    public boolean isMain() {
        return main;
    }

    /**
     * The display's current mode.
     *
     * @return the current mode, or empty if it could not be read
     */
    public Optional<DisplayMode> getMode() {
        return Optional.ofNullable(mode);
    }

    /**
     * Builds a display's current mode from CoreGraphics values.
     *
     * @param x           the x origin from {@code CGDisplayBounds}, in points
     * @param y           the y origin from {@code CGDisplayBounds}, in points
     * @param width       the width from {@code CGDisplayBounds}, in points, in the current orientation
     * @param height      the height from {@code CGDisplayBounds}, in points, in the current orientation
     * @param pixelWidth  {@code CGDisplayModeGetPixelWidth} of the current mode, which is unrotated
     * @param pixelHeight {@code CGDisplayModeGetPixelHeight} of the current mode, which is unrotated
     * @param refreshRate {@code CGDisplayModeGetRefreshRate} of the current mode; 0 for a panel with no fixed rate
     * @param rotation    {@code CGDisplayRotation}, in degrees clockwise
     * @return the mode, with the pixel size turned to the current orientation
     */
    public static DisplayMode toMode(double x, double y, double width, double height, // NOSONAR java:S107
            long pixelWidth, long pixelHeight, double refreshRate, double rotation) {
        int degrees = (int) Math.round(rotation);
        boolean quarterTurn = Math.abs(degrees) % 180 == 90;
        int pw = (int) (quarterTurn ? pixelHeight : pixelWidth);
        int ph = (int) (quarterTurn ? pixelWidth : pixelHeight);
        return new DisplayModeImpl((int) Math.round(x), (int) Math.round(y), (int) Math.round(width),
                (int) Math.round(height), pw, ph, refreshRate, degrees);
    }

    /**
     * Finds the CoreGraphics display that reports the same identity as an EDID.
     *
     * @param displays the active CoreGraphics displays
     * @param edid     the EDID of a display enumerated through IOKit
     * @return the one display whose vendor, model and serial numbers match the EDID's, or empty if none do or if more
     *         than one does, as happens with two identical monitors that report no serial number
     */
    public static Optional<CoreGraphicsDisplay> matchEdid(List<CoreGraphicsDisplay> displays, byte[] edid) {
        // EDID bytes 8-9: big-endian manufacturer ID; 10-11: little-endian product code; 12-15: little-endian serial
        if (edid.length < 16) {
            return Optional.empty();
        }
        int vendor = ((edid[8] & 0xff) << 8) | (edid[9] & 0xff);
        int model = (edid[10] & 0xff) | ((edid[11] & 0xff) << 8);
        int serial = (edid[12] & 0xff) | ((edid[13] & 0xff) << 8) | ((edid[14] & 0xff) << 16)
                | ((edid[15] & 0xff) << 24);
        CoreGraphicsDisplay match = null;
        for (CoreGraphicsDisplay display : displays) {
            if (display.vendor == vendor && display.model == model && display.serial == serial) {
                if (match != null) {
                    return Optional.empty();
                }
                match = display;
            }
        }
        return Optional.ofNullable(match);
    }

    /**
     * Finds the CoreGraphics display that is built in.
     *
     * @param displays the active CoreGraphics displays
     * @return the one built-in display, or empty if there is none or more than one
     */
    public static Optional<CoreGraphicsDisplay> matchBuiltIn(List<CoreGraphicsDisplay> displays) {
        CoreGraphicsDisplay match = null;
        for (CoreGraphicsDisplay display : displays) {
            if (display.builtIn) {
                if (match != null) {
                    return Optional.empty();
                }
                match = display;
            }
        }
        return Optional.ofNullable(match);
    }
}
