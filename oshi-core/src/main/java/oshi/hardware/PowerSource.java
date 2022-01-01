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
package oshi.hardware;

import java.time.LocalDate;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * The Power Source is one or more batteries with some capacity, and some state
 * of charge/discharge
 */
@ThreadSafe
public interface PowerSource {
    /**
     * Units of Battery Capacity
     */
    enum CapacityUnits {
        /**
         * MilliWattHours (mWh).
         */
        MWH,

        /**
         * MilliAmpHours (mAh). Should be multiplied by voltage to convert to mWh.
         */
        MAH,

        /**
         * Relative units. The specific units are not defined. The ratio of current/max
         * capacity still represents state of charge and the ratio of max/design
         * capacity still represents state of health.
         */
        RELATIVE;
    }

    /**
     * Name of the power source at the Operating System level.
     *
     * @return The power source name, as reported by the operating system.
     */
    String getName();

    /**
     * Name of the power source at the device level.
     *
     * @return The power source name, as reported by the device itself.
     */
    String getDeviceName();

    /**
     * Estimated remaining capacity as a fraction of max capacity.
     * <p>
     * This is an estimated/smoothed value which should correspond to the Operating
     * System's "percent power" display, and may not directly correspond to the
     * ratio of {@link #getCurrentCapacity()} to {@link #getMaxCapacity()}.
     *
     * @return A value between 0.0 (fully drained) and 1.0 (fully charged)
     */
    double getRemainingCapacityPercent();

    /**
     * Estimated time remaining on the power source, in seconds, as reported by the
     * operating system.
     * <p>
     * This is an estimated/smoothed value which should correspond to the Operating
     * System's "battery time remaining" display, and will react slowly to changes
     * in power consumption.
     *
     * @return If positive, seconds remaining. If negative, -1.0 (calculating) or
     *         -2.0 (unlimited)
     */
    double getTimeRemainingEstimated();

    /**
     * Estimated time remaining on the power source, in seconds, as reported by the
     * battery. If the battery is charging, this value may represent time remaining
     * to fully charge the battery.
     * <p>
     * Note that this value is not very accurate on some battery systems. The value
     * may vary widely depending on present power usage, which could be affected by
     * disk activity and other factors. This value will often be a higher value than
     * {@link #getTimeRemainingEstimated()}.
     *
     * @return Seconds remaining to fully discharge or fully charge the battery.
     */
    double getTimeRemainingInstant();

    /**
     * Power Usage Rate of the battery, in milliWatts (mW).
     *
     * @return If positive, the charge rate. If negative, the discharge rate.
     */
    double getPowerUsageRate();

    /**
     * Voltage of the battery, in Volts.
     *
     * @return the battery voltage, or -1 if unknown.
     */
    double getVoltage();

    /**
     * Amperage of the battery, in milliAmperes (mA).
     *
     * @return the battery amperage. If positive, charging the battery. If negative,
     *         discharging the battery.
     */
    double getAmperage();

    /**
     * Reports whether the device is plugged in to an external power source.
     *
     * @return {@code true} if plugged in, {@code false} otherwise.
     */
    boolean isPowerOnLine();

    /**
     * Reports whether the battery is charging.
     *
     * @return {@code true} if the battery is charging, {@code false} otherwise.
     */
    boolean isCharging();

    /**
     * Reports whether the battery is discharging.
     *
     * @return {@code true} if the battery is discharging, {@code false} otherwise.
     */
    boolean isDischarging();

    /**
     * Reports =the units of {@link #getCurrentCapacity()},
     * {@link #getMaxCapacity()}, and {@link #getDesignCapacity()}
     *
     * @return The units of battery capacity.
     */
    CapacityUnits getCapacityUnits();

    /**
     * The current (remaining) capacity of the battery.
     *
     * @return The current capacity. Units are defined by
     *         {@link #getCapacityUnits()}.
     */
    int getCurrentCapacity();

    /**
     * The maximum capacity of the battery. When compared to design capacity,
     * permits a measure of battery state of health. It is possible for max capacity
     * to exceed design capacity.
     *
     * @return The maximum capacity. Units are defined by
     *         {@link #getCapacityUnits()}.
     */
    int getMaxCapacity();

    /**
     * The design (original) capacity of the battery. When compared to maximum
     * capacity, permits a measure of battery state of health. It is possible for
     * max capacity to exceed design capacity.
     *
     * @return The design capacity. Units are defined by
     *         {@link #getCapacityUnits()}.
     */
    int getDesignCapacity();

    /**
     * The cycle count of the battery, if known.
     *
     * @return The cycle count of the battery, or -1 if unknown.
     */
    int getCycleCount();

    /**
     * The battery chemistry (e.g., Lithium Ion).
     *
     * @return the battery chemistry.
     */
    String getChemistry();

    /**
     * The battery's date of manufacture.
     * <p>
     * Some battery manufacturers encode the manufacture date in the serial number.
     * Parsing this value is operating system and battery manufacturer dependent,
     * and is left to the user.
     *
     * @return the manufacture date, if available. May be {@code null}.
     */
    LocalDate getManufactureDate();

    /**
     * The name of the battery's manufacturer.
     *
     * @return the manufacturer name.
     */
    String getManufacturer();

    /**
     * The battery's serial number.
     * <p>
     * Some battery manufacturers encode the manufacture date in the serial number.
     * Parsing this value is operating system and battery manufacturer dependent,
     * and is left to the user.
     *
     * @return the serial number.
     */
    String getSerialNumber();

    /**
     * The battery's temperature, in degrees Celsius.
     *
     * @return the battery's temperature, or 0 if uknown.
     */
    double getTemperature();

    /**
     * Updates statistics on this battery.
     *
     * @return {@code true} if the update was successful. If {@code false} the
     *         battery statistics are unchanged.
     */
    boolean updateAttributes();
}
