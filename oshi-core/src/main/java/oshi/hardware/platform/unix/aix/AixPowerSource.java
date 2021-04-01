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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;

/**
 * A Power Source
 */
@ThreadSafe
public final class AixPowerSource extends AbstractPowerSource {

    public AixPowerSource(String name, String deviceName, double remainingCapacityPercent,
            double timeRemainingEstimated, double timeRemainingInstant, double powerUsageRate, double voltage,
            double amperage, boolean powerOnLine, boolean charging, boolean discharging, CapacityUnits capacityUnits,
            int currentCapacity, int maxCapacity, int designCapacity, int cycleCount, String chemistry,
            LocalDate manufactureDate, String manufacturer, String serialNumber, double temperature) {
        super(name, deviceName, remainingCapacityPercent, timeRemainingEstimated, timeRemainingInstant, powerUsageRate,
                voltage, amperage, powerOnLine, charging, discharging, capacityUnits, currentCapacity, maxCapacity,
                designCapacity, cycleCount, chemistry, manufactureDate, manufacturer, serialNumber, temperature);
    }

    /**
     * Gets Battery Information. AIX does not provide any battery statistics, as
     * most servers are not designed to be run on battery.
     *
     * @return An empty list.
     */
    public static List<PowerSource> getPowerSources() {
        return Collections.emptyList();
    }
}
