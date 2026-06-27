/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * Display refers to the information regarding a video source and monitor identified by the EDID standard.
 * <p>
 * The {@link #getEdid()} method returns the raw EDID (Extended Display Identification Data) byte array, a 128-byte (or
 * longer) structure that monitors transmit to describe their capabilities. Use the {@link oshi.util.EdidUtil} class to
 * parse this byte array into human-readable fields such as manufacturer ID, product ID, serial number, display
 * dimensions, and supported resolutions.
 * <p>
 * Example: extracting monitor manufacturer and dimensions:
 *
 * <pre>{@code
 * for (Display display : hal.getDisplays()) {
 *     byte[] edid = display.getEdid();
 *     System.out.println("Manufacturer: " + EdidUtil.getManufacturerID(edid));
 *     System.out.println("Product ID:   " + EdidUtil.getProductID(edid));
 *     int hCm = EdidUtil.getHcm(edid);
 *     int vCm = EdidUtil.getVcm(edid);
 *     System.out.printf("Size: %d cm x %d cm%n", hCm, vCm);
 * }
 * }</pre>
 *
 * For displays that report their attributes without providing an EDID (such as a built-in macOS Retina panel),
 * {@link #isEdidSynthetic()} returns {@code true} and {@link #getEdid()} returns an EDID synthesized from those
 * attributes. The decoded fields are also available directly through {@link #getDisplayInfo()}.
 *
 * @see oshi.util.EdidUtil
 * @see DisplayInfo
 */
@PublicApi
@Immutable
public interface Display {
    /**
     * The EDID byte array.
     *
     * @return The EDID byte array, either reported by the display or, when {@link #isEdidSynthetic()} is {@code true},
     *         synthesized from the display's reported attributes.
     */
    byte[] getEdid();

    /**
     * The decoded display information, equivalent to parsing {@link #getEdid()} with {@link oshi.util.EdidUtil}.
     *
     * @return A {@link DisplayInfo} holding the display's decoded attributes.
     */
    DisplayInfo getDisplayInfo();

    /**
     * Indicates whether the EDID returned by {@link #getEdid()} was synthesized from the display's reported attributes
     * rather than read directly from the display.
     *
     * @return {@code true} if the EDID is synthetic, {@code false} if it is the display's raw EDID.
     */
    boolean isEdidSynthetic();
}
