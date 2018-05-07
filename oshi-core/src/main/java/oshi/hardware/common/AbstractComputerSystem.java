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
