package oshi.hardware.common;

import oshi.hardware.Firmware;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:59
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
public abstract class AbstractFirmware implements Firmware {

    private String manufacturer;
    private String name;
    private String description;
    private String version;
    private Date releaseDate;

    protected AbstractFirmware() {

        this.manufacturer = "unknown";
        this.name = "unknown";
        this.description = "unknown";
        this.version = "unknown";
        this.releaseDate = null;
    }

    @Override
    public final String getManufacturer() {
        return manufacturer;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getDescription() {
        return description;
    }

    @Override
    public final String getVersion() {
        return version;
    }

    @Override
    public final Date getReleaseDate() {
        return releaseDate;
    }

    protected final void setManufacturer(final String manufacturer) {
        this.manufacturer = manufacturer;
    }

    protected final void setName(final String name) {
        this.name = name;
    }

    protected final void setDescription(final String description) {
        this.description = description;
    }

    protected final void setVersion(final String version) {
        this.version = version;
    }

    protected final void setReleaseDate(final Date releaseDate) {
        this.releaseDate = releaseDate;
    }
}
