/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.hardware.common.AbstractSensors;
import oshi.util.FileUtil;

public class LinuxSensors extends AbstractSensors {
    private static final Logger LOG = LoggerFactory.getLogger(LinuxSensors.class);

    // Possible sensor types. See sysfs documentation for others, e.g. current
    private static final String[] SENSORS = { "temp", "fan", "in" };

    // Base HWMON path, adds 0, 1, etc. to end for various sensors
    private static final String HWMON = "/sys/class/hwmon/hwmon";

    // Map from sensor to path
    private Map<String, String> hwmonMap = new HashMap<String, String>();

    public LinuxSensors() {
        // Iterate over all hwmon* directories and look for sensor files
        // e.g. /sys/class/hwmon/hwmon0/temp1_input
        int i = 0;
        while (Files.isDirectory(Paths.get(HWMON + i))) {
            for (String sensor : SENSORS) {
                String path = HWMON + i + "/" + sensor;
                File sensorFile = new File(path + "1_input");
                if (sensorFile.exists()) {
                    hwmonMap.put(sensor, path);
                }
            }
            i++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        if (hwmonMap.containsKey("temp")) {
            String hwmon = hwmonMap.get("temp");
            List<String> tempInfo = null;
            try {
                tempInfo = FileUtil.readFile(hwmon + "1_input");
            } catch (IOException e) {
                LOG.error("Problem with {}{}: {}", hwmon, "1_input", e.getMessage());
                return 0d;
            }
            // Should return a single line of millidegrees Celsius
            if (tempInfo.size() > 0) {
                int millidegrees = 0;
                try {
                    millidegrees = Integer.parseInt(tempInfo.get(0));
                } catch (NumberFormatException e) {
                    LOG.error("Invalid format for temperature: {}", tempInfo.get(0));
                }
                return millidegrees / 1000d;
            }
        }
        return 0d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        if (hwmonMap.containsKey("fan")) {
            String hwmon = hwmonMap.get("fan");
            List<Integer> speeds = new ArrayList<Integer>();
            int fan = 1;
            for (;;) {
                List<String> fanInfo = null;
                try {
                    fanInfo = FileUtil.readFile(hwmon + fan + "_input");
                } catch (IOException e) {
                    // No file found, we've reached max fans
                    break;
                }
                // Should return a single line of RPM
                if (fanInfo.size() > 0) {
                    int rpm = 0;
                    try {
                        rpm = Integer.parseInt(fanInfo.get(0));
                    } catch (NumberFormatException e) {
                        LOG.error("Invalid format for fan speed: {}", fanInfo.get(0));
                    }
                    speeds.add(rpm);
                }
            }
            int[] fanSpeeds = new int[speeds.size()];
            for (int i = 0; i < speeds.size(); i++) {
                fanSpeeds[i] = speeds.get(i);
            }
            return fanSpeeds;
        }
        return new int[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuVoltage() {
        if (hwmonMap.containsKey("in")) {
            String hwmon = hwmonMap.get("in");
            List<String> voltInfo = null;
            try {
                voltInfo = FileUtil.readFile(hwmon + "1_input");
            } catch (IOException e) {
                LOG.error("Problem with {}{}: {}", hwmon, "1_input", e.getMessage());
                return 0d;
            }
            // Should return a single line of millidegrees Celsius
            if (voltInfo.size() > 0) {
                int millivolts = 0;
                try {
                    millivolts = Integer.parseInt(voltInfo.get(0));
                } catch (NumberFormatException e) {
                    LOG.error("Invalid format for temperature: {}", voltInfo.get(0));
                }
                return millivolts / 1000d;
            }
        }
        return 0d;
    }
}
