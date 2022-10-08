/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.Baseboard;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Firmware;

/**
 * Computer System data.
 */
@Immutable
public abstract class AbstractComputerSystem implements ComputerSystem {

    private final Supplier<Firmware> firmware = memoize(this::createFirmware);

    private final Supplier<Baseboard> baseboard = memoize(this::createBaseboard);

    @Override
    public Firmware getFirmware() {
        return firmware.get();
    }

    /**
     * Instantiates the platform-specific {@link Firmware} object
     *
     * @return platform-specific {@link Firmware} object
     */
    protected abstract Firmware createFirmware();

    @Override
    public Baseboard getBaseboard() {
        return baseboard.get();
    }

    /**
     * Instantiates the platform-specific {@link Baseboard} object
     *
     * @return platform-specific {@link Baseboard} object
     */
    protected abstract Baseboard createBaseboard();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("manufacturer=").append(getManufacturer()).append(", ");
        sb.append("model=").append(getModel()).append(", ");
        sb.append("serial number=").append(getSerialNumber()).append(", ");
        sb.append("uuid=").append(getHardwareUUID());
        return sb.toString();
    }
}
