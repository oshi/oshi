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
 * <p>
 * What the display is, as described by its EDID, is on {@link DisplayInfo}. How it is attached and how the windowing
 * system is currently driving it, such as {@link #getDevicePort()} and {@link #getCurrentMode()}, is on this interface.
 *
 * @see DisplayInfo
 * @see DisplayMode
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
     * The mode the windowing system is currently driving this display in: its size, pixel resolution, refresh rate,
     * rotation, and position on the desktop.
     * <p>
     * The display must be matched to the windowing system's own view of it, which is done lazily on the first call:
     * <ul>
     * <li>macOS: by comparing the EDID's vendor, product and serial numbers with those CoreGraphics reports. Two
     * connected monitors that report identical numbers cannot be told apart, and neither returns a mode.</li>
     * <li>Windows: by the monitor's device path, through the Connecting and Configuring Displays (CCD) API.</li>
     * <li>Linux and the other UNIX platforms: by the {@code xrandr} output that {@link #getOutputName()} names, so an X
     * server with the RandR extension must be reachable.</li>
     * </ul>
     * A {@link Display} is a snapshot, so the mode is read once and not refreshed; query the displays again to see a
     * change.
     *
     * @return An {@link Optional} containing the current mode, or empty if the display is not active on the desktop or
     *         cannot be matched to the windowing system.
     */
    default Optional<DisplayMode> getCurrentMode() {
        return Optional.empty();
    }

    /**
     * Whether this display is built into the device, such as a laptop panel, rather than an external monitor.
     * <ul>
     * <li>macOS: CoreGraphics' own classification for a display it can match, and known directly for the Apple Silicon
     * built-in panel and external ports.</li>
     * <li>Windows: from the output technology the CCD API reports for the display's connector, where embedded
     * DisplayPort, LVDS, embedded UDI and internal connections are built in.</li>
     * <li>Linux and the other UNIX platforms: from the connector name, where {@code eDP}, {@code LVDS} and {@code DSI}
     * connectors are built in.</li>
     * </ul>
     *
     * @return An {@link Optional} containing {@code true} if the display is built in, {@code false} if it is external,
     *         or empty if it cannot be determined.
     */
    default Optional<Boolean> isBuiltIn() {
        return Optional.empty();
    }

    /**
     * Whether this display is the primary display: the one the windowing system treats as the main desktop, placing it
     * at the desktop origin and, on macOS, giving it the menu bar.
     * <p>
     * As for {@link #getCurrentMode()}, the display must be matched to the windowing system's own view of it, which is
     * done lazily on the first call:
     * <ul>
     * <li>macOS: CoreGraphics' main display, as {@code CGDisplayIsMain} reports it. A display is matched by comparing
     * the EDID's vendor, product and serial numbers with those CoreGraphics reports, and the Apple Silicon built-in
     * panel by its built-in flag. Two active monitors that report identical numbers cannot be told apart, and neither
     * returns a value; if CoreGraphics lists only one of them as active, both are matched to it and report its
     * status.</li>
     * <li>Windows: the display whose desktop position in its current mode is the origin, which is how Windows defines
     * the primary display. A display that is attached but not active on the desktop has no current mode, and returns no
     * value.</li>
     * <li>Linux and the other UNIX platforms: the {@code xrandr} output marked {@code primary}, matched as
     * {@link #getOutputName()} describes, so an X server with the RandR extension must be reachable. Under Wayland
     * without an X server, or on a headless system, no value is returned.</li>
     * </ul>
     * Exactly one display reporting {@code true} is not guaranteed. On Windows, a display cloning the primary display
     * shares its desktop position and also reports {@code true}. X11 need not designate a primary output at all, in
     * which case every output reports {@code false}.
     * <p>
     * A {@link Display} is a snapshot, so the status is read once and not refreshed; query the displays again to see a
     * change.
     *
     * @return An {@link Optional} containing {@code true} if the display is the primary display, {@code false} if it is
     *         not, or empty if it cannot be determined because the display cannot be matched to the windowing system.
     */
    default Optional<Boolean> isPrimary() {
        return Optional.empty();
    }
}
