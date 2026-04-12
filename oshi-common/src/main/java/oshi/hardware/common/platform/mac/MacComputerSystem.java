/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.Immutable;
import oshi.hardware.common.AbstractComputerSystem;
import oshi.util.tuples.Quartet;

/**
 * Hardware data obtained from ioreg.
 */
@Immutable
public abstract class MacComputerSystem extends AbstractComputerSystem {

    private final Supplier<Quartet<String, String, String, String>> manufacturerModelSerialUUID = memoize(
            this::platformExpert);

    @Override
    public String getManufacturer() {
        return manufacturerModelSerialUUID.get().getA();
    }

    @Override
    public String getModel() {
        return manufacturerModelSerialUUID.get().getB();
    }

    @Override
    public String getSerialNumber() {
        return manufacturerModelSerialUUID.get().getC();
    }

    @Override
    public String getHardwareUUID() {
        return manufacturerModelSerialUUID.get().getD();
    }

    protected abstract Quartet<String, String, String, String> platformExpert();
}
