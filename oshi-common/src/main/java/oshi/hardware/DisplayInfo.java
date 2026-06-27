/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * Holds the human-readable information described by a display's EDID (Extended Display Identification Data), exposing
 * the same values that {@link oshi.util.EdidUtil} parses from a raw EDID byte array.
 * <p>
 * For a display that reports a raw EDID, {@link #getEdid()} returns those bytes and {@link #isEdidSynthetic()} is
 * {@code false}. For a display that reports its attributes without an EDID (such as a built-in macOS Retina panel),
 * {@link #isEdidSynthetic()} is {@code true} and {@link #getEdid()} returns an EDID synthesized from those attributes.
 * <p>
 * A synthesized EDID is a valid 128-byte EDID (correct header and checksum) that re-parses to the same values this
 * interface exposes, so it is interchangeable with a real EDID for {@link oshi.util.EdidUtil} decoding. It carries only
 * those values, however: unlike a panel's real EDID it does not include the additional detail (chromaticity
 * coordinates, full detailed-timing parameters, extension blocks, etc.) that was never reported.
 *
 * @see oshi.util.EdidUtil
 */
@PublicApi
@Immutable
public interface DisplayInfo {

    /**
     * Gets the EDID byte array: the bytes reported by the display, or, when {@link #isEdidSynthetic()} is {@code true},
     * an EDID synthesized from the display's reported attributes.
     *
     * @return A copy of the EDID byte array.
     */
    byte[] getEdid();

    /**
     * Indicates whether the EDID returned by {@link #getEdid()} was synthesized from reported attributes rather than
     * read directly from the display.
     *
     * @return {@code true} if the EDID is synthetic, {@code false} if it is the display's raw EDID.
     */
    boolean isEdidSynthetic();

    /**
     * Gets the manufacturer ID.
     *
     * @return The manufacturer ID.
     */
    String getManufacturerID();

    /**
     * Gets the product ID.
     *
     * @return The product ID.
     */
    String getProductID();

    /**
     * Gets the serial number, the numeric ID serial number from bytes 12-15 of the EDID. This is distinct from
     * {@link #getProductSerialNumber()}.
     *
     * @return The serial number.
     */
    String getSerialNo();

    /**
     * Gets the week of manufacture.
     *
     * @return The week of manufacture.
     */
    byte getWeek();

    /**
     * Gets the year of manufacture.
     *
     * @return The year of manufacture.
     */
    int getYear();

    /**
     * Gets the EDID version.
     *
     * @return The EDID version.
     */
    String getVersion();

    /**
     * Tests whether the display is digital.
     *
     * @return {@code true} if the display is digital, {@code false} otherwise.
     */
    boolean isDigital();

    /**
     * Gets the monitor width in cm.
     *
     * @return The monitor width in cm.
     */
    int getHcm();

    /**
     * Gets the monitor height in cm.
     *
     * @return The monitor height in cm.
     */
    int getVcm();

    /**
     * Gets the preferred resolution.
     *
     * @return The preferred resolution (e.g. {@code "2560x1440"}).
     */
    String getPreferredResolution();

    /**
     * Gets the monitor model.
     *
     * @return The monitor model.
     */
    String getModel();

    /**
     * Gets the display product serial number, the text of the serial-number descriptor. This is distinct from
     * {@link #getSerialNo()}, which returns the numeric ID serial number.
     *
     * @return The display product serial number, or an empty string if not available.
     */
    String getProductSerialNumber();
}
