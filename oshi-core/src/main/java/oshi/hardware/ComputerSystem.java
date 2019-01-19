/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware;

import java.io.Serializable;

/**
 * The ComputerSystem represents the physical hardware, of a computer
 * system/product and includes BIOS/firmware and a motherboard, logic board,
 * etc.
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
     * <P>
     * Performs a best-effort attempt to retrieve a unique serial number from
     * the computer system. This may originate from the baseboard, BIOS,
     * processor, hardware UUID, etc.
     * <P>
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
     * Get the computer system firmware/BIOS.
     *
     * @return A {@link Firmware} object for this system
     */
    Firmware getFirmware();

    /**
     * Get the computer system baseboard/motherboard.
     *
     * @return A {@link Baseboard} object for this system
     */
    Baseboard getBaseboard();

}
