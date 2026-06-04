/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.openbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.unix.bsd.Systat;
import oshi.hardware.common.AbstractSensors;
import oshi.util.tuples.Triplet;

/**
 * Sensors
 */
@ThreadSafe
public final class OpenBsdSensors extends AbstractSensors {

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
