/*
 * MIT License
 *
 * Copyright (c) 2019-2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
