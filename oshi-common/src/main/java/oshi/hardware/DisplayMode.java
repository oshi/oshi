/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * The mode a display is being driven in, as the windowing system reports it: its size on the desktop, the native pixel
 * resolution behind that size, its refresh rate, its rotation, and where it sits on the desktop.
 * <p>
 * All sizes are in the display's current orientation, so a 1920x1080 panel rotated a quarter turn reports a width of
 * 1080 and a height of 1920.
 * <p>
 * The logical size ({@link #getWidth()}, {@link #getHeight()}) is the area the display occupies in desktop coordinates,
 * and the pixel size ({@link #getPixelWidth()}, {@link #getPixelHeight()}) is the resolution actually sent to the
 * panel. They differ where the windowing system scales the desktop, such as a macOS Retina display, where a 3456x2234
 * panel may be presented as 1728x1117 points, or an X output configured with {@code xrandr --scale}. Where no scaling
 * applies they are equal.
 *
 * @see Display#getCurrentMode()
 */
@PublicApi
@Immutable
public interface DisplayMode {

    /**
     * The width of the area this display occupies on the desktop, in logical units (points on macOS, pixels elsewhere).
     *
     * @return the logical width
     */
    int getWidth();

    /**
     * The height of the area this display occupies on the desktop, in logical units (points on macOS, pixels
     * elsewhere).
     *
     * @return the logical height
     */
    int getHeight();

    /**
     * The width of the resolution driven to the display, in native pixels.
     *
     * @return the pixel width
     */
    int getPixelWidth();

    /**
     * The height of the resolution driven to the display, in native pixels.
     *
     * @return the pixel height
     */
    int getPixelHeight();

    /**
     * The refresh rate.
     *
     * @return the refresh rate in hertz, or 0 if not known or if the display reports no fixed rate
     */
    double getRefreshRate();

    /**
     * The rotation of the display relative to its natural orientation.
     *
     * @return the rotation in degrees clockwise: 0, 90, 180 or 270
     */
    int getRotation();

    /**
     * The horizontal position of this display's top-left corner on the desktop, in logical units. All displays share
     * one origin: on macOS and Windows it is the top-left corner of the primary display, so a display to its left has a
     * negative value; under X11 it is the top-left corner of the X screen, and values are never negative.
     *
     * @return the x coordinate of the display's origin
     */
    int getX();

    /**
     * The vertical position of this display's top-left corner on the desktop, in logical units. All displays share one
     * origin, as described for {@link #getX()}.
     *
     * @return the y coordinate of the display's origin
     */
    int getY();
}
