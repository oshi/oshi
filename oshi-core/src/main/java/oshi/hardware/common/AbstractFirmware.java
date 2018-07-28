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

import oshi.hardware.Firmware;

/**
 * Firmware data
 *
 * @author SchiTho1 [at] Securiton AG
 * @author widdis [at] gmail [dot] com
 */
public abstract class AbstractFirmware implements Firmware {

    private static final long serialVersionUID = 1L;

    private String manufacturer;
    private String name;
    private String description;
    private String version;
    private String releaseDate;

    private static final String UNKNOWN = "unknown";

    public AbstractFirmware() {
        this.manufacturer = UNKNOWN;
        this.name = UNKNOWN;
        this.description = UNKNOWN;
        this.version = UNKNOWN;
        this.releaseDate = UNKNOWN;
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
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return this.description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVersion() {
        return this.version;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReleaseDate() {
        return this.releaseDate;
    }

    /**
     * @param manufacturer
     *            The manufacturer to set.
     */
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param description
     *            The description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param version
     *            The version to set.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @param releaseDate
     *            The releaseDate to set.
     */
    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

}
