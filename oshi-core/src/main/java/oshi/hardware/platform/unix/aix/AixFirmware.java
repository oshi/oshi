/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;

/**
 * Firmware data.
 */
@Immutable
final class AixFirmware extends AbstractFirmware {

    private final String manufacturer;
    private final String name;
    private final String version;

    AixFirmware(String manufacturer, String name, String version) {
        this.manufacturer = manufacturer;
        this.name = name;
        this.version = version;
    }

    @Override
    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }
}
