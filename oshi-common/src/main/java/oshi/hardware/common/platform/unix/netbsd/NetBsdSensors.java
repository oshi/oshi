/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.netbsd.Envstat;
import oshi.hardware.common.AbstractSensors;
import oshi.util.tuples.Triplet;

/**
 * Sensors from NetBSD's {@code envsys(4)} framework, read with {@code envstat}.
 */
@ThreadSafe
final class NetBsdSensors extends AbstractSensors {

    private final Supplier<Triplet<Double, int[], Double>> tempFanVolts = memoize(
            () -> Envstat.summarize(Envstat.querySensors()), defaultExpiration());

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
