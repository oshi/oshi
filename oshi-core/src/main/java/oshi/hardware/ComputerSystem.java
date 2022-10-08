/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.Immutable;

/**
 * The ComputerSystem represents the physical hardware, of a computer system/product and includes BIOS/firmware and a
 * motherboard, logic board, etc.
 */
@Immutable
public interface ComputerSystem {
    /**
     * Get the computer system manufacturer.
     *
     * @return The manufacturer.
     */
    String getManufacturer();

    /**
     * Get the computer system model.
     *
     * @return The model.
     */
    String getModel();

    /**
     * Get the computer system serial number, if available.
     * <P>
     * Performs a best-effort attempt to retrieve a unique serial number from the computer system. This may originate
     * from the baseboard, BIOS, processor, etc.
     * <P>
     * This value is provided for information only. Caution should be exercised if using this result to "fingerprint" a
     * system for licensing or other purposes, as the result may change based on program permissions or installation of
     * software packages. Specifically, on Linux and FreeBSD, this requires either root permissions, or installation of
     * the (deprecated) HAL library (lshal command). Linux also attempts to read the dmi/id serial number files in
     * sysfs, which are read-only root by default but may have permissions altered by the user.
     *
     * @return the System Serial Number, if available, otherwise returns "unknown"
     */
    String getSerialNumber();

    /**
     * Get the computer system hardware UUID, if available.
     * <P>
     * Performs a best-effort attempt to retrieve the hardware UUID.
     *
     * @return the Hardware UUID, if available, otherwise returns "unknown"
     */
    String getHardwareUUID();

    /**
     * Get the computer system firmware/BIOS.
     *
     * @return A {@link oshi.hardware.Firmware} object for this system
     */
    Firmware getFirmware();

    /**
     * Get the computer system baseboard/motherboard.
     *
     * @return A {@link oshi.hardware.Baseboard} object for this system
     */
    Baseboard getBaseboard();

}
