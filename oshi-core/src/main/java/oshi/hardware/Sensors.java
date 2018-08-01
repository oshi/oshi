/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
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
