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
}
