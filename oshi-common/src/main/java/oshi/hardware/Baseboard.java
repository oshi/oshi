/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * The Baseboard represents the system board, also called motherboard, logic board, etc.
 * <p>
 * Obtained from {@link ComputerSystem#getBaseboard()}.
 * <p>
 * <b>Platform notes:</b> On Linux, reading the serial number requires root access because
 * {@code /sys/class/dmi/id/board_serial} is root-owned and read-only (mode 0400) by default.
 *
 * @see ComputerSystem
 */
@PublicApi
@Immutable
public interface Baseboard {
    /**
     * Get the baseboard manufacturer.
     *
     * @return The manufacturer.
     */
    String getManufacturer();

    /**
     * Get the baseboard model.
     *
     * @return The model.
     */
    String getModel();

    /**
     * Get the baseboard version.
     *
     * @return The version.
     */
    String getVersion();

    /**
     * Get the baseboard serial number.
     *
     * @return The serial number.
     */
    String getSerialNumber();
}
