/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common;

import static oshi.util.Memoizer.defaultExpiration;
import static oshi.util.Memoizer.memoize;

import java.util.Arrays;
import java.util.function.Supplier;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.Sensors;

/**
 * Sensors from WMI or Open Hardware Monitor
 */
@ThreadSafe
public abstract class AbstractSensors implements Sensors {

    private final Supplier<Double> cpuTemperature = memoize(this::queryCpuTemperature, defaultExpiration());

    private final Supplier<int[]> fanSpeeds = memoize(this::queryFanSpeeds, defaultExpiration());

    private final Supplier<Double> cpuVoltage = memoize(this::queryCpuVoltage, defaultExpiration());

    @Override
    public double getCpuTemperature() {
        return cpuTemperature.get();
    }

    protected abstract double queryCpuTemperature();

    @Override
    public int[] getFanSpeeds() {
        return fanSpeeds.get();
    }

    protected abstract int[] queryFanSpeeds();

    @Override
    public double getCpuVoltage() {
        return cpuVoltage.get();
    }

    protected abstract double queryCpuVoltage();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CPU Temperature=").append(getCpuTemperature()).append("°C, ");
        sb.append("Fan Speeds=").append(Arrays.toString(getFanSpeeds())).append(", ");
        sb.append("CPU Voltage=").append(getCpuVoltage());
        return sb.toString();
    }
}
