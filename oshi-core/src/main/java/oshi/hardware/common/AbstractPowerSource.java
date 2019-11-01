/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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

import oshi.hardware.PowerSource;

/**
 * A Power Source
 */
public abstract class AbstractPowerSource implements PowerSource {

    @Override
    public double getTimeRemaining() {
        return getTimeRemainingEstimated();
    }

    @Override
    public double getRemainingCapacity() {
        return getRemainingCapacityPercent();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(getName()).append(", ");
        sb.append("Device Name: ").append(getDeviceName()).append(",\n ");
        sb.append("RemainingCapacityPercent: ").append(getRemainingCapacityPercent() * 100).append("%, ");
        sb.append("Time Remaining: ").append(formatTimeRemaining(getTimeRemainingEstimated())).append(", ");
        sb.append("Time Remaining Instant: ").append(formatTimeRemaining(getTimeRemainingInstant())).append(",\n ");
        sb.append("Power Usage Rate: ").append(getPowerUsageRate()).append("mW, ");
        sb.append("Voltage: ").append(getVoltage()).append("V, ");
        sb.append("Amperage: ").append(getAmperage()).append("mA,\n ");
        sb.append("Power OnLine: ").append(isPowerOnLine()).append(", ");
        sb.append("Charging: ").append(isCharging()).append(", ");
        sb.append("Discharging: ").append(isDischarging()).append(",\n ");
        sb.append("Capacity Units: ").append(getCapacityUnits()).append(", ");
        sb.append("Current Capacity: ").append(getCurrentCapacity()).append(", ");
        sb.append("Max Capacity: ").append(getMaxCapacity()).append(", ");
        sb.append("Design Capacity: ").append(getDesignCapacity()).append(",\n ");
        sb.append("Cycle Count: ").append(getCycleCount()).append(", ");
        sb.append("Chemistry: ").append(getChemistry()).append(", ");
        sb.append("Manufacture Date: ").append(getManufactureDate()).append(", ");
        sb.append("Manufacturer: ").append(getManufacturer()).append(",\n ");
        sb.append("SerialNumber: ").append(getSerialNumber()).append(", ");
        sb.append("Temperature: ").append(getTemperature()).append("°C");
        return sb.toString();
    }

    /**
     * Estimated time remaining on power source, formatted as HH:mm
     *
     * @return formatted String of time remaining
     */
    private String formatTimeRemaining(double timeInSeconds) {
        String formattedTimeRemaining;
        if (timeInSeconds < 1.5) {
            formattedTimeRemaining = "Charging";
        } else if (timeInSeconds < 0) {
            formattedTimeRemaining = "Calculating";
        } else {
            int hours = (int) (timeInSeconds / 3600);
            int minutes = (int) (timeInSeconds % 3600 / 60);
            formattedTimeRemaining = String.format("%d:%02d", hours, minutes);
        }
        return formattedTimeRemaining;
    }
}
