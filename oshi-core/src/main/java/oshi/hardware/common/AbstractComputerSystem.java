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
package oshi.hardware.common;

import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;

/**
 * Hardware data
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
public abstract class AbstractComputerSystem implements ComputerSystem {

    private static final long serialVersionUID = 1L;

    private String manufacturer;
    private String model;
    private String serialNumber;
    private Firmware firmware;
    private Baseboard baseboard;

    protected AbstractComputerSystem() {
        this.manufacturer = "unknown";
        this.model = "unknown";
        this.serialNumber = "unknown";
        this.firmware = null;
        this.baseboard = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManufacturer() {
        return this.manufacturer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getModel() {
        return this.model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Firmware getFirmware() {
        return this.firmware;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Baseboard getBaseboard() {
        return this.baseboard;
    }

    /**
     * @param manufacturer
     *            The manufacturer to set.
     */
    protected void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * @param model
     *            The model to set.
     */
    protected void setModel(String model) {
        this.model = model;
    }

    /**
     * @param serialNumber
     *            The serialNumber to set.
     */
    protected void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * @param firmware
     *            The firmware to set.
     */
    protected void setFirmware(Firmware firmware) {
        this.firmware = firmware;
    }

    /**
     * @param baseboard
     *            The baseboard to set.
     */
    protected void setBaseboard(Baseboard baseboard) {
        this.baseboard = baseboard;
    }

}
