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
package oshi.hardware;

import java.io.Serializable;

/**
 * Sensors include hardwore sensors to monitor temperature, fan speed, and other
 * information. Drivers may or may not exist to collect this data depending on
 * Operating System and CPU. In addition, software-hardware communication may
 * suffer intermittent errors when attempting to access this information, so it
 * should be considered for information only. Users should test for zero values
 * and empty arrays which will result if the OS is unable to provide the
 * information.
 *
 * Windows information is retrieved via Windows Management Instrumentation
 * (WMI). Unfortunately, most hardware providers do not publish values to WMI.
 * If a value is not available through the Microsoft API, Oshi will attempt to
 * retrieve values as published by the Open Hardware Monitor
 * (http://openhardwaremonitor.org/) if it is running.
 *
 * @author widdis[at]gmail[dot]com
 */
public interface Sensors extends Serializable {
    /**
     * CPU Temperature
     *
     * @return CPU Temperature in degrees Celsius if available, 0 otherwise.
     */
    double getCpuTemperature();

    /**
     * Fan speeds
     *
     * @return Speed in rpm for all fans. May return empty array if no fans
     *         detected or 0 fan speed if unable to measure fan speed.
     */
    int[] getFanSpeeds();

    /**
     * CPU Voltage
     *
     * @return CPU Voltage in Volts if available, 0 otherwise.
     */
    double getCpuVoltage();
}
