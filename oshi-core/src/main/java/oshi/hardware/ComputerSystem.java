/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.hardware;

import java.io.Serializable;

/**
 * The ComputerSystem represents the physical hardware, of a computer
 * system/product and includes BIOS/firmware and a motherboard, logic board,
 * etc.
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
public interface ComputerSystem extends Serializable {
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
     *
     * Performs a best-effort attempt to retrieve a unique serial number from
     * the computer system. This may originate from the baseboard, BIOS,
     * processor, hardware UUID, etc.
     *
     * This value is provided for information only. Caution should be exercised
     * if using this result to "fingerprint" a system for licensing or other
     * purposes, as the result may change based on program permissions or
     * installation of software packages. Specifically, on Linux and FreeBSD,
     * this requires either root permissions, or installation of the
     * (deprecated) HAL library (lshal command). Linux also attempts to read the
     * dmi/id serial number files in sysfs, which are read-only root by default
     * but may have permissions altered by the user.
     *
     * @return the System Serial Number, if available, otherwise returns
     *         "unknown"
     */
    String getSerialNumber();

    /**
     * Get the computer system firmware/BIOS
     *
     * @return A {@link Firmware} object for this system
     */
    Firmware getFirmware();

    /**
     * Get the computer system baseboard/motherboard
     *
     * @return A {@link Baseboard} object for this system
     */
    Baseboard getBaseboard();

}
