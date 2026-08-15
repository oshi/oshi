/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.unix.aix.Lscfg;
import oshi.hardware.common.AbstractBaseboard;
import oshi.util.ParseUtil;
import oshi.util.tuples.Triplet;

/**
 * Baseboard data obtained by lscfg
 */
@Immutable
public final class AixBaseboard extends AbstractBaseboard {

    private static final String IBM = "IBM";
    private final String model;
    private final String serialNumber;
    private final String version;

    AixBaseboard(Supplier<List<String>> lscfg) {
        Triplet<@Nullable String, @Nullable String, @Nullable String> msv = Lscfg
                .queryBackplaneModelSerialVersion(lscfg.get());
        this.model = ParseUtil.getStringValueOrUnknown(msv.getA());
        this.serialNumber = ParseUtil.getStringValueOrUnknown(msv.getB());
        this.version = ParseUtil.getStringValueOrUnknown(msv.getC());
    }

    @Override
    public String getManufacturer() {
        return IBM;
    }

    @Override
    public String getModel() {
        return this.model;
    }

    @Override
    public String getSerialNumber() {
        return this.serialNumber;
    }

    @Override
    public String getVersion() {
        return this.version;
    }
}
