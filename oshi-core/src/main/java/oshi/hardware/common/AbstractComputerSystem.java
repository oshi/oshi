/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
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
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.common;

import oshi.hardware.ComputerSystem;

/**
 * Hardware data
 * 
 * @author SchiTho1 @ Securiton AG
 */
public abstract class AbstractComputerSystem implements ComputerSystem {

    private String manufacturer;
    private String model;
    private String serialNumber;

    protected AbstractComputerSystem() {
        this.manufacturer = "unknown";
        this.model = "unknown";
        this.serialNumber = "unknown";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getManufacturer() {
        return manufacturer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getModel() {
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getSerialNumber() {
        return serialNumber;
    }

    protected final void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    protected final void setModel(final String model) {
        this.model = model;
    }

    protected final void setSerialNumber(final String serialNumber) {
        this.serialNumber = serialNumber;
    }
}
