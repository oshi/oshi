/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

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

    /**
     * Constructs a new {@code AixPowerSource}.
     *
     * @param name                     the power source name
     * @param deviceName               the device name
     * @param remainingCapacityPercent the remaining capacity as a fraction of the maximum
     * @param timeRemainingEstimated   the estimated time remaining in seconds
     * @param timeRemainingInstant     the reported instantaneous time remaining in seconds
     * @param powerUsageRate           the power usage rate in milliwatts
     * @param voltage                  the voltage in volts
     * @param amperage                 the amperage in milliamperes
     * @param powerOnLine              whether external power is connected
     * @param charging                 whether the power source is charging
     * @param discharging              whether the power source is discharging
     * @param capacityUnits            the units of the capacity values
     * @param currentCapacity          the current capacity
     * @param maxCapacity              the maximum capacity
     * @param designCapacity           the design capacity
     * @param cycleCount               the charge cycle count
     * @param chemistry                the battery chemistry
     * @param manufactureDate          the manufacture date
     * @param manufacturer             the manufacturer
     * @param serialNumber             the serial number
     * @param temperature              the temperature in degrees Celsius
     */
    public AixPowerSource(String name, String deviceName, double remainingCapacityPercent,
            double timeRemainingEstimated, double timeRemainingInstant, double powerUsageRate, double voltage,
            double amperage, boolean powerOnLine, boolean charging, boolean discharging, CapacityUnits capacityUnits,
            int currentCapacity, int maxCapacity, int designCapacity, int cycleCount, String chemistry,
            LocalDate manufactureDate, String manufacturer, String serialNumber, double temperature) {
        super(name, deviceName, remainingCapacityPercent, timeRemainingEstimated, timeRemainingInstant, powerUsageRate,
                voltage, amperage, powerOnLine, charging, discharging, capacityUnits, currentCapacity, maxCapacity,
                designCapacity, cycleCount, chemistry, manufactureDate, manufacturer, serialNumber, temperature);
    }

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
    }

    /**
     * Gets Battery Information. AIX does not provide any battery statistics, as most servers are not designed to be run
     * on battery.
     *
     * @return An empty list.
     */
    public static List<PowerSource> getPowerSources() {
        return Collections.emptyList();
    }
}
