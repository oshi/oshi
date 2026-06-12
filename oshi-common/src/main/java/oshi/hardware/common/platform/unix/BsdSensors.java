/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.bsd.Systat;
import oshi.hardware.common.AbstractSensors;
import oshi.util.tuples.Triplet;

/**
 * Shared sensors implementation for BSDs that use {@code systat} for temperature, fan speed, and voltage (NetBSD,
 * OpenBSD).
 */
@ThreadSafe
public final class BsdSensors extends AbstractSensors {

    private final Supplier<Triplet<Double, int[], Double>> tempFanVolts = memoize(Systat::querySensors,
            defaultExpiration());

    @Override
    public double queryCpuTemperature() {
        return tempFanVolts.get().getA();
    }

    @Override
    public int[] queryFanSpeeds() {
        return tempFanVolts.get().getB();
    }

    @Override
    public double queryCpuVoltage() {
        return tempFanVolts.get().getC();
    }
}
