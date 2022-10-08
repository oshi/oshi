/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;

/**
 * Firmware data.
 */
@Immutable
final class SolarisFirmware extends AbstractFirmware {

    private final String manufacturer;
    private final String version;
    private final String releaseDate;

    SolarisFirmware(String manufacturer, String version, String releaseDate) {
        this.manufacturer = manufacturer;
        this.version = version;
        this.releaseDate = releaseDate;
    }

    @Override
    public String getManufacturer() {
        return manufacturer;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getReleaseDate() {
        return releaseDate;
    }
}
