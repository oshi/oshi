package oshi.hardware.common;

import oshi.hardware.Assembly;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:58
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
public abstract class AbstractAssembly implements Assembly {

    private String manufacturer;
    private String model;
    private String serialNumber;

    protected AbstractAssembly() {
        this.manufacturer = "unknown";
        this.model = "unknown";
        this.serialNumber = "unknown";
    }

    @Override
    public final String getManufacturer() {
        return manufacturer;
    }

    @Override
    public final String getModel() {
        return model;
    }

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
