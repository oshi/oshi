/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
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
