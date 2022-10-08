/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * The Firmware represents the low level BIOS or equivalent.
 */
@Immutable
public interface Firmware {

    /**
     * Get the firmware manufacturer.
     *
     * @return the manufacturer
     */
    String getManufacturer();

    /**
     * Get the firmware name.
     *
     * @return the name
     */
    String getName();

    /**
     * Get the firmware description.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Get the firmware version.
     *
     * @return the version
     */
    String getVersion();

    /**
     * Get the firmware release date.
     *
     * @return The release date.
     */
    String getReleaseDate();
}
