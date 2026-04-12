/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractFirmware;
import oshi.util.tuples.Quintet;

/**
 * Firmware data obtained from ioreg. Subclasses provide platform-specific IOKit queries.
 */
@Immutable
public abstract class MacFirmware extends AbstractFirmware {

    private final Supplier<Quintet<String, String, String, String, String>> manufNameDescVersRelease = memoize(
            this::queryEfi);

    @Override
    public String getManufacturer() {
        return manufNameDescVersRelease.get().getA();
    }

    @Override
    public String getName() {
        return manufNameDescVersRelease.get().getB();
    }

    @Override
    public String getDescription() {
        return manufNameDescVersRelease.get().getC();
    }

    @Override
    public String getVersion() {
        return manufNameDescVersRelease.get().getD();
    }

    @Override
    public String getReleaseDate() {
        return manufNameDescVersRelease.get().getE();
    }

    protected abstract Quintet<String, String, String, String, String> queryEfi();
}
