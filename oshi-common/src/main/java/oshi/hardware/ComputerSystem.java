/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.PublicApi;
import oshi.annotation.concurrent.Immutable;

/**
 * The ComputerSystem represents the physical hardware, of a computer system/product and includes BIOS/firmware and a
 * motherboard, logic board, etc.
 * <p>
 * <b>Docker/container note:</b> OSHI reports the <em>host</em> operating system and hardware information, not the
 * container's. Serial numbers and UUIDs may return "unknown" inside containers.
 * <p>
 * <b>Unique machine identifier:</b> The {@link #getHardwareUUID()} value can be combined with other fields (such as
 * processor ID and serial number) to construct a machine fingerprint. Note that the UUID value
 * {@code 03000200-0400-0500-0006-000700080009} is a known placeholder that is not unique. See the {@code ComputerID}
 * class in the {@code oshi-demo} module for an example approach.
 * <p>
 * <b>VM detection:</b> Virtual machine environments can often be identified by examining the
 * {@link #getManufacturer()}, {@link #getModel()}, and {@link Firmware} values. See the {@code DetectVM} class in the
 * {@code oshi-demo} module for an example.
 */
@PublicApi
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
