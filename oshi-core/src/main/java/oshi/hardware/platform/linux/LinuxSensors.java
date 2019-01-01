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
package oshi.hardware.platform.linux;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.hardware.Sensors;
import oshi.util.FileUtil;

public class LinuxSensors implements Sensors {

    private static final long serialVersionUID = 1L;

    // Possible sensor types. See sysfs documentation for others, e.g. current
    private static final String TEMP = "temp";
    private static final String FAN = "fan";
    private static final String VOLTAGE = "in";
    private static final String[] SENSORS = { TEMP, FAN, VOLTAGE };

    // Base HWMON path, adds 0, 1, etc. to end for various sensors
    private static final String HWMON = "/sys/class/hwmon/hwmon";
    // Base THERMAL_ZONE path, adds 0, 1, etc. to end for temperature sensors
    private static final String THERMAL_ZONE = "/sys/class/thermal/thermal_zone";

    // Map from sensor to path
    private Map<String, String> sensorsMap = new HashMap<>();

    public LinuxSensors() {
        // Iterate over all hwmon* directories and look for sensor files
        // e.g. /sys/class/hwmon/hwmon0/temp1_input
        for (String sensor : SENSORS) {
            // Final to pass to anonymous class
            final String sensorPrefix = sensor;
            getSensorFilesFromPath(HWMON, sensor, new FileFilter() {
                // Find any *_input files in that path
                @Override
                public boolean accept(File f) {
                    return f.getName().startsWith(sensorPrefix) && f.getName().endsWith("_input")
                            && FileUtil.getIntFromFile(f.getName()) > 0;
                }
            });
        }
        // Iterate over all thermal_zone* directories and look for sensor files
        // if no temperature sensor is found
        // e.g. /sys/class/thermal/thermal_zone0/temp
        if (!this.sensorsMap.containsKey(TEMP)) {
            getSensorFilesFromPath(THERMAL_ZONE, TEMP, new FileFilter() {
                // Find any temp files in that path
                @Override
                public boolean accept(File f) {
                    return f.getName().equals(TEMP);
                }
            });
        }
    }

    /**
     * Find all sensor files in a specific path and adds them to the hwmonMap
     *
     * @param sensorPath
     *            A string containing the sensor path
     * @param sensor
     *            A string containing the sensor
     * @param sensorFileFilter
     *            A FileFilter for detecting valid sensor files
     */
    private void getSensorFilesFromPath(String sensorPath, String sensor, FileFilter sensorFileFilter) {
        int i = 0;
        while (Paths.get(sensorPath + i).toFile().isDirectory()) {
            String path = sensorPath + i;
            File dir = new File(path);
            File[] matchingFiles = dir.listFiles(sensorFileFilter);
            if (matchingFiles != null && matchingFiles.length > 0) {
                this.sensorsMap.put(sensor, String.format("%s/%s", path, sensor));
            }
            i++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getCpuTemperature() {
        long millidegrees = 0;
        if (!this.sensorsMap.containsKey(TEMP)) {
            return 0d;
        }
        String hwmon = this.sensorsMap.get(TEMP);
        if (hwmon.contains("hwmon")) {
            // First attempt should be CPU temperature at index 1, if available
            millidegrees = FileUtil.getLongFromFile(String.format("%s1_input", hwmon));
            // Should return a single line of millidegrees Celsius
            if (millidegrees > 0) {
                return millidegrees / 1000d;
            }
            // If temp1_input doesn't exist, iterate over temp2..temp6_input
            // and average
            long sum = 0;
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
        } else if (hwmon.contains("thermal_zone")) {
            // If temp2..temp6_input doesn't exist, try thermal_zone0
            millidegrees = FileUtil.getLongFromFile(hwmon);
            // Should return a single line of millidegrees Celsius
            if (millidegrees > 0) {
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
        if (this.sensorsMap.containsKey(FAN)) {
            String hwmon = this.sensorsMap.get(FAN);
            List<Integer> speeds = new ArrayList<>();
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
        if (this.sensorsMap.containsKey(VOLTAGE)) {
            String hwmon = this.sensorsMap.get(VOLTAGE);
            // Should return a single line of millivolt
            return FileUtil.getIntFromFile(String.format("%s1_input", hwmon)) / 1000d;
        }
        return 0d;
    }
}