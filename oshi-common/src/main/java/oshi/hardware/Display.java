/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import java.util.Optional;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;
import oshi.util.Constants;

/**
 * Display refers to the information regarding a video source and monitor identified by the EDID standard.
 * <p>
 * {@link #getDisplayInfo()} returns a {@link DisplayInfo} exposing the display's decoded attributes — manufacturer ID,
 * product ID, serial number, physical dimensions, preferred resolution, model name, and the raw (or synthesized) EDID
 * byte array.
 * <p>
 * Example: extracting monitor manufacturer and dimensions:
 *
 * <pre>{@code
 * for (Display display : hal.getDisplays()) {
 *     DisplayInfo info = display.getDisplayInfo();
 *     System.out.println("Manufacturer: " + info.getManufacturerID());
 *     System.out.println("Product ID:   " + info.getProductID());
 *     System.out.printf("Size: %d cm x %d cm%n", info.getHcm(), info.getVcm());
 * }
 * }</pre>
 *
 * For displays that report their attributes without providing an EDID (such as a built-in macOS Retina panel),
 * {@link DisplayInfo#isEdidSynthetic()} returns {@code true} and {@link DisplayInfo#getEdid()} returns an EDID
 * synthesized from those attributes.
 *
 * @see DisplayInfo
 * @see oshi.util.EdidUtil
 */
@PublicApi
@Immutable
public interface Display {
    /**
     * The EDID byte array.
     *
     * @return The EDID byte array, either reported by the display or, when {@link DisplayInfo#isEdidSynthetic()} is
     *         {@code true}, synthesized from the display's reported attributes.
     * @deprecated As of 7.4.0, use {@link #getDisplayInfo()}.{@link DisplayInfo#getEdid() getEdid()} instead; the
     *             decoded attributes are also available directly from {@link DisplayInfo}. Scheduled for removal in the
     *             next major release.
     */
    @Deprecated
    byte[] getEdid();

    /**
     * The decoded display information.
     *
     * @return A {@link DisplayInfo} holding the display's decoded attributes.
     */
    DisplayInfo getDisplayInfo();

    /**
     * The system-level device identification for this display. The form of the identifier is platform-specific:
     * <ul>
     * <li>Linux: the DRM connector name from sysfs (e.g. {@code HDMI-A-1}, {@code eDP-1}, {@code DP-2}), or the
     * {@code xrandr} output name when DRM sysfs is not available.</li>
     * <li>macOS: on Apple Silicon, the port named by an external monitor's framebuffer {@code TransportDescription}
     * (e.g. {@code Port-HDMI@1}, {@code Port-USB-C@1}), or the built-in panel's device tree name (e.g. {@code disp0}).
     * Intel Macs do not expose a port.</li>
     * <li>Windows: the connector derived from the Connecting and Configuring Displays (CCD) API's output technology and
     * connector instance (e.g. {@code HDMI}, {@code DisplayPort-1}).</li>
     * <li>Other UNIX platforms: the {@code xrandr} output name, which is the same value {@link #getOutputName()}
     * returns.</li>
     * </ul>
     *
     * @return The device port identifier, or {@link Constants#UNKNOWN} if not available.
     */
    default String getDevicePort() {
        return Constants.UNKNOWN;
    }

    /**
     * The X11 output name for this display as reported by {@code xrandr} (e.g. {@code HDMI-1}, {@code DP2}). This is
     * the name to pass to {@code xrandr --output}. Implemented on Linux and the other UNIX platforms, and only
     * available where an X server with the RandR extension is reachable. On Linux the display is matched to an X output
     * by its DRM {@code CONNECTOR_ID}, falling back to a comparison of their EDIDs.
     *
     * @return An {@link Optional} containing the xrandr output name, or empty if not available.
     */
    default Optional<String> getOutputName() {
        return Optional.empty();
    }

    /**
     * Whether this display is the primary display. Returns {@code true} only when the platform positively identifies
     * this display as primary; returns {@code false} when the primary display cannot be determined (e.g. under Wayland,
     * on headless systems, or on platforms that do not expose primary-display information).
     *
     * @return {@code true} if this display is the primary display, {@code false} otherwise
     */
    default boolean isPrimary() {
        return false;
    }
}
