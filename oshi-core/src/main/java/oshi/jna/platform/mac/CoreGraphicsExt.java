/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.jna.platform.mac;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.mac.CoreGraphics;

/**
 * Extensions to JNA's {@link CoreGraphics} for functions not yet bound upstream. This class should be considered
 * non-API as it may be removed if/when its code is incorporated into the JNA project.
 */
public interface CoreGraphicsExt extends CoreGraphics {

    CoreGraphicsExt INSTANCE = Native.load("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics",
            CoreGraphicsExt.class);

    /**
     * A {@link com.sun.jna.platform.mac.CoreGraphics.CGSize CGSize} returned by value from native functions.
     */
    @FieldOrder({ "width", "height" })
    class CGSizeByValue extends Structure implements Structure.ByValue {
        public double width;
        public double height;
    }

    /**
     * Returns the physical size of the display in millimeters.
     *
     * @param display The display identifier.
     * @return A {@link CGSizeByValue} containing the width and height in millimeters.
     */
    CGSizeByValue CGDisplayScreenSize(int display);

    /**
     * Returns the display's current mode. The caller must release it with {@link #CGDisplayModeRelease(Pointer)}.
     *
     * @param display The display identifier.
     * @return A {@code CGDisplayModeRef}, or {@code null} if the display is invalid.
     */
    Pointer CGDisplayCopyDisplayMode(int display);

    /**
     * Returns the width of a display mode in pixels. The return type is {@code size_t}, 64 bits on every macOS
     * architecture.
     *
     * @param mode The display mode.
     * @return The width in pixels.
     */
    long CGDisplayModeGetPixelWidth(Pointer mode);

    /**
     * Returns the height of a display mode in pixels. The return type is {@code size_t}, 64 bits on every macOS
     * architecture.
     *
     * @param mode The display mode.
     * @return The height in pixels.
     */
    long CGDisplayModeGetPixelHeight(Pointer mode);

    /**
     * Returns the refresh rate of a display mode.
     *
     * @param mode The display mode.
     * @return The refresh rate in hertz, or 0 for a display with no fixed rate.
     */
    double CGDisplayModeGetRefreshRate(Pointer mode);

    /**
     * Releases a display mode.
     *
     * @param mode The display mode.
     */
    void CGDisplayModeRelease(Pointer mode);
}
