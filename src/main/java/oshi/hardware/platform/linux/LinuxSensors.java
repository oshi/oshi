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
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.common.AbstractSensors;
import oshi.util.FileUtil;

public class LinuxSensors extends AbstractSensors {
    // Possible sensor types. See sysfs documentation for others, e.g. current
    private static final String TEMP = "temp";
    private static final String FAN = "fan";
    private static final String VOLTAGE = "in";
    private static final String[] SENSORS = { TEMP, FAN, VOLTAGE };

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
                String path = String.format("%s%d", HWMON, i);
                // Final to pass to anonymous class
                final String prefix = sensor;
                // Find any *_input files in that path
                File dir = new File(path);
                File[] matchingFiles = dir.listFiles(new FileFilter() {
                    public boolean accept(File pathname) {
                        return pathname.getName().startsWith(prefix) && pathname.getName().endsWith("_input");
                    }
                });
                if (matchingFiles != null && matchingFiles.length > 0) {
                    hwmonMap.put(sensor, String.format("%s/%s", path, sensor));
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
        if (hwmonMap.containsKey(TEMP)) {
            String hwmon = hwmonMap.get(TEMP);
            // First attempt should be CPU temperature at index 1, if available
            long millidegrees = FileUtil.getLongFromFile(String.format("%s1_input", hwmon));
            // Should return a single line of millidegrees Celsius
            if (millidegrees > 0) {
                return millidegrees / 1000d;
            } else {
                // If temp1_input doesn't exist, iterate over temp2..temp6_input
                // and average
                int sum = 0;
                int count = 0;
                for (int i = 2; i <= 6; i++) {
                    millidegrees = FileUtil.getLongFromFile(String.format("%s%d_input", hwmon, i));
                    if (millidegrees > 0) {
                        sum += millidegrees;
                        count++;
                    }
                }
                if (count > 0) {
                    return sum / (count * 1000d);
                }
            }
        }
        return 0d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getFanSpeeds() {
        if (hwmonMap.containsKey(FAN)) {
            String hwmon = hwmonMap.get(FAN);
            List<Integer> speeds = new ArrayList<Integer>();
            int fan = 1;
            for (;;) {
                String fanPath = String.format("%s%d_input", hwmon, fan);
                if (!new File(fanPath).exists()) {
                    // No file found, we've reached max fans
                    break;
                }
                // Should return a single line of RPM
                speeds.add(FileUtil.getIntFromFile(fanPath));
                // Done reading data for current fan, read next fan
                fan++;
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
        if (hwmonMap.containsKey(VOLTAGE)) {
            String hwmon = hwmonMap.get(VOLTAGE);
            // Should return a single line of millidegrees Celsius
            return FileUtil.getIntFromFile(String.format("%s1_input", hwmon)) / 1000d;
        }
        return 0d;
    }
}
