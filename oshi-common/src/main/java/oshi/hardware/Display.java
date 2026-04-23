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
 * @see oshi.util.EdidUtil
 */
@PublicApi
@Immutable
public interface Display {
    /**
     * The EDID byte array.
     *
     * @return The original unparsed EDID byte array.
     */
    byte[] getEdid();
}
