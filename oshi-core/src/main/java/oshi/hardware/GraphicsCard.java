/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * <p>
 * GraphicsCard interface.
 * </p>
 */
@Immutable
public interface GraphicsCard {

    /**
     * Retrieves the full name of the card.
     *
     * @return The name of the card.
     */
    String getName();

    /**
     * Retrieves the card's Device ID
     *
     * @return The Device ID of the card
     */
    String getDeviceId();

    /**
     * Retrieves the card's manufacturer/vendor
     *
     * @return The vendor of the card as human-readable text if possible, or the Vendor ID (VID) otherwise
     */
    String getVendor();

    /**
     * Retrieves a list of version/revision data from the card. Users may need to further parse this list to identify
     * specific GPU capabilities.
     *
     * @return A comma-delimited list of version/revision data
     */
    String getVersionInfo();

    /**
     * Retrieves the Video RAM (VRAM) available on the GPU
     *
     * @return Total number of bytes.
     */
    long getVRam();
}
