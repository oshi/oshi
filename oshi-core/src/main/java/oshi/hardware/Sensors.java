/*
 * Copyright 2016-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Sensors include hardware sensors to monitor temperature, fan speed, and other information.
 * <p>
 * Drivers may or may not exist to collect this data depending on the installed hardware and Operating System. In
 * addition, software-hardware communication may suffer intermittent errors when attempting to access this information.
 * Users should expect, test for, and handle zero values and/or empty arrays which will result if the OS is unable to
 * provide the information.
 * <p>
 * Windows information is generally retrieved via Windows Management Instrumentation (WMI). Unfortunately, most hardware
 * providers do not publish sensor values to WMI. OSHI attempts to retrieve values from
 * <a href="https://github.com/LibreHardwareMonitor/LibreHardwareMonitor">LibreHardwareMonitor</a> if the optional
 * <a href="https://github.com/pandalxb/jLibreHardwareMonitor">jLibreHardwareMonitor</a> dependency is included. Otherwise, OSHI attempts to retrieve values from
 * <a href="https://openhardwaremonitor.org/">Open Hardware Monitor</a> if it is running. Otherwise, OSHI retrieves via
 * the Microsoft API, which may require elevated permissions and still may provide no results or unchanging results
 * depending on the motherboard manufacturer.
 */
@ThreadSafe
public interface Sensors {
    /**
     * CPU Temperature
     *
     * @return CPU Temperature in degrees Celsius if available, 0 or {@link Double#NaN} otherwise.
     *         <p>
     *         See notes on {@link Sensors}.
     */
    double getCpuTemperature();

    /**
     * Fan speeds
     *
     * @return Speed in rpm for all fans. May return empty array if no fans detected or 0 fan speed if unable to measure
     *         fan speed.
     */
    int[] getFanSpeeds();

    /**
     * CPU Voltage
     *
     * @return CPU Voltage in Volts if available, 0 otherwise.
     */
    double getCpuVoltage();
}
