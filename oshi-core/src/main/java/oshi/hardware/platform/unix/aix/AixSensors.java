/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.aix;

import java.util.List;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;

/**
 * Sensors not available except counting fans from lscfg
 */
@ThreadSafe
final class AixSensors extends AbstractSensors {

    private final Supplier<List<String>> lscfg;

    AixSensors(Supplier<List<String>> lscfg) {
        this.lscfg = lscfg;
    }

    @Override
    public double queryCpuTemperature() {
        // Not available in general without specialized software
        return 0d;
    }

    @Override
    public int[] queryFanSpeeds() {
        // Speeds are not available in general without specialized software
        // We can count fans from lscfg and return an appropriate sized array of zeroes.
        int fans = 0;
        for (String s : lscfg.get()) {
            if (s.contains("Air Mover")) {
                fans++;
            }
        }
        return new int[fans];
    }

    @Override
    public double queryCpuVoltage() {
        // Not available in general without specialized software
        return 0d;
    }
}
