/*
 * Copyright 2016-2024 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractSensors;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.GlobalConfig;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.SysPath;

/**
 * Sensors from WMI or Open Hardware Monitor
 */
@ThreadSafe
final class LinuxSensors extends AbstractSensors {

    public static final String OSHI_THERMAL_ZONE_TYPE_PRIORITY = "oshi.os.linux.sensors.cpuTemperature.types";

    private static final List<String> THERMAL_ZONE_TYPE_PRIORITY = Stream
            .of(GlobalConfig.get(OSHI_THERMAL_ZONE_TYPE_PRIORITY, "").split(","))
            .filter((s) -> !s.isEmpty())
            .collect(Collectors.toList());

    private static final String TYPE = "type";
    // Possible sensor types. See sysfs documentation for others, e.g. current
    private static final String TEMP = "temp";
    private static final String FAN = "fan";
    private static final String VOLTAGE = "in";
    private static final String[] SENSORS = { TEMP, FAN, VOLTAGE };

    // Base HWMON path, adds 0, 1, etc. to end for various sensors
    private static final String HWMON = "hwmon";
    private static final String HWMON_PATH = SysPath.HWMON + HWMON;
    // Base THERMAL_ZONE path, adds 0, 1, etc. to end for temperature sensors
    private static final String THERMAL_ZONE = "thermal_zone";
    private static final String THERMAL_ZONE_PATH = SysPath.THERMAL + THERMAL_ZONE;

    // Initial test to see if we are running on a Pi
    private static final boolean IS_PI = queryCpuTemperatureFromVcGenCmd() > 0;

    // Map from sensor to path. Built by constructor, so thread safe
    private final Map<String, String> sensorsMap = new HashMap<>();

    /**
     * <p>
     * Constructor for LinuxSensors.
     * </p>
     */
    LinuxSensors() {
        if (!IS_PI) {
            populateSensorsMapFromHwmon();
            // if no temperature sensor is found in hwmon, try thermal_zone
            if (!this.sensorsMap.containsKey(TEMP)) {
                populateSensorsMapFromThermalZone();
            }
        }
    }

    /*
     * Iterate over all hwmon* directories and look for sensor files, e.g., /sys/class/hwmon/hwmon0/temp1_input
     */
    private void populateSensorsMapFromHwmon() {
        for (String sensor : SENSORS) {
            // Final to pass to anonymous class
            final String sensorPrefix = sensor;
            // Find any *_input files in that path
            getSensorFilesFromPath(HWMON_PATH, sensor, f -> {
                try {
                    return f.getName().startsWith(sensorPrefix) && f.getName().endsWith("_input")
                            && FileUtil.getIntFromFile(f.getCanonicalPath()) > 0;
                } catch (IOException e) {
                    return false;
                }
            });
        }
    }

    /*
     * Iterate over all thermal_zone* directories and look for sensor files, e.g., /sys/class/thermal/thermal_zone0/temp
     */
    private void populateSensorsMapFromThermalZone() {
        getSensorFilesFromPath(THERMAL_ZONE_PATH, TEMP,
                f -> f.getName().equals(TYPE) || f.getName().equals(TEMP),
                files -> Stream.of(files)
                        .filter(f -> TYPE.equals(f.getName()))
                        .findFirst()
                        .map(File::getPath)
                        .map(FileUtil::getStringFromFile)
                        .map(THERMAL_ZONE_TYPE_PRIORITY::indexOf)
                        .filter((index) -> index >= 0)
                        .orElse(THERMAL_ZONE_TYPE_PRIORITY.size()));
    }

    /**
     * Find all sensor files in a specific path and adds them to the sensorsMap
     *
     * @param sensorPath       A string containing the sensor path
     * @param sensor           A string containing the sensor
     * @param sensorFileFilter A FileFilter for detecting valid sensor files
     */
    private void getSensorFilesFromPath(String sensorPath, String sensor, FileFilter sensorFileFilter) {
        getSensorFilesFromPath(sensorPath, sensor, sensorFileFilter, (files) -> 0);
    }

    /**
     * Find all sensor files in a specific path and adds them to the sensorsMap
     *
     * @param sensorPath       A string containing the sensor path
     * @param sensor           A string containing the sensor
     * @param sensorFileFilter A FileFilter for detecting valid sensor files
     * @param prioritizer      A callback to prioritize between multiple sensors
     */
    private void getSensorFilesFromPath(String sensorPath, String sensor, FileFilter sensorFileFilter, ToIntFunction<File[]> prioritizer) {
        String selectedPath = null;
        int selectedPriority = Integer.MAX_VALUE;

        int i = 0;
        while (Paths.get(sensorPath + i).toFile().isDirectory()) {
            String path = sensorPath + i;
            File dir = new File(path);
            File[] matchingFiles = dir.listFiles(sensorFileFilter);

            if (matchingFiles != null && matchingFiles.length > 0) {
                int priority = prioritizer.applyAsInt(matchingFiles);

                if (priority < selectedPriority) {
                    selectedPriority = priority;
                    selectedPath = path;
                }
            }
            i++;
        }

        if (selectedPath != null) {
            this.sensorsMap.put(sensor, String.format(Locale.ROOT, "%s/%s", selectedPath, sensor));
        }
    }

    @Override
    public double queryCpuTemperature() {
        if (IS_PI) {
            return queryCpuTemperatureFromVcGenCmd();
        }
        String tempStr = this.sensorsMap.get(TEMP);
        if (tempStr != null) {
            long millidegrees = 0;
            if (tempStr.contains(HWMON)) {
                // First attempt should be CPU temperature at index 1, if available
                millidegrees = FileUtil.getLongFromFile(String.format(Locale.ROOT, "%s1_input", tempStr));
                // Should return a single line of millidegrees Celsius
                if (millidegrees > 0) {
                    return millidegrees / 1000d;
                }
                // If temp1_input doesn't exist, iterate over temp2..temp6_input
                // and average
                long sum = 0;
                int count = 0;
                for (int i = 2; i <= 6; i++) {
                    millidegrees = FileUtil.getLongFromFile(String.format(Locale.ROOT, "%s%d_input", tempStr, i));
                    if (millidegrees > 0) {
                        sum += millidegrees;
                        count++;
                    }
                }
                if (count > 0) {
                    return sum / (count * 1000d);
                }
            } else if (tempStr.contains(THERMAL_ZONE)) {
                // If temp2..temp6_input doesn't exist, try thermal_zone0
                millidegrees = FileUtil.getLongFromFile(tempStr);
                // Should return a single line of millidegrees Celsius
                if (millidegrees > 0) {
                    return millidegrees / 1000d;
                }
            }
        }
        return 0d;
    }

    /**
     * Retrieves temperature from Raspberry Pi
     *
     * @return The temperature on a Pi, 0 otherwise
     */
    private static double queryCpuTemperatureFromVcGenCmd() {
        String tempStr = ExecutingCommand.getFirstAnswer("vcgencmd measure_temp");
        // temp=50.8'C
        if (tempStr.startsWith("temp=")) {
            return ParseUtil.parseDoubleOrDefault(tempStr.replaceAll("[^\\d|\\.]+", ""), 0d);
        }
        return 0d;
    }

    @Override
    public int[] queryFanSpeeds() {
        if (!IS_PI) {
            String fanStr = this.sensorsMap.get(FAN);
            if (fanStr != null) {
                List<Integer> speeds = new ArrayList<>();
                int fan = 1;
                for (;;) {
                    String fanPath = String.format(Locale.ROOT, "%s%d_input", fanStr, fan);
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
        }
        return new int[0];
    }

    @Override
    public double queryCpuVoltage() {
        if (IS_PI) {
            return queryCpuVoltageFromVcGenCmd();
        }
        String voltageStr = this.sensorsMap.get(VOLTAGE);
        if (voltageStr != null) {
            // Should return a single line of millivolt
            return FileUtil.getIntFromFile(String.format(Locale.ROOT, "%s1_input", voltageStr)) / 1000d;
        }
        return 0d;
    }

    /**
     * Retrieves voltage from Raspberry Pi
     *
     * @return The temperature on a Pi, 0 otherwise
     */
    private static double queryCpuVoltageFromVcGenCmd() {
        // For raspberry pi
        String voltageStr = ExecutingCommand.getFirstAnswer("vcgencmd measure_volts core");
        // volt=1.20V
        if (voltageStr.startsWith("volt=")) {
            return ParseUtil.parseDoubleOrDefault(voltageStr.replaceAll("[^\\d|\\.]+", ""), 0d);
        }
        return 0d;
    }
}
