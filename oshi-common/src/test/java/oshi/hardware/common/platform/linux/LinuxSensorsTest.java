/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LinuxSensorsTest {

    private static final double EPS = 1e-6;

    @Test
    void testNoSensorsFound(@TempDir Path tempDir) {
        // No hwmon or thermal_zone directories exist
        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        assertThat(sensors.queryCpuTemperature(), closeTo(0d, EPS));
        assertThat(sensors.queryFanSpeeds().length, is(0));
        assertThat(sensors.queryCpuVoltage(), closeTo(0d, EPS));
    }

    @Test
    void testIsPiReturnsDefaults() {
        // isPi=true but vcgencmd won't work on test machine, so returns 0/empty
        LinuxSensors sensors = new LinuxSensors("/nonexistent/hwmon", "/nonexistent/thermal_zone", true);
        assertThat(sensors.queryCpuTemperature(), closeTo(0d, EPS));
        assertThat(sensors.queryFanSpeeds().length, is(0));
        assertThat(sensors.queryCpuVoltage(), closeTo(0d, EPS));
    }

    @Test
    void testHwmonTemperatureTemp1(@TempDir Path tempDir) throws IOException {
        Path hwmon0 = createHwmonDir(tempDir, 0, "coretemp");
        writeFile(hwmon0.resolve("temp1_input"), "52000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // 52000 millidegrees = 52.0 C
        assertThat(sensors.queryCpuTemperature(), closeTo(52.0, EPS));
    }

    @Test
    void testHwmonTemperatureAveraging(@TempDir Path tempDir) throws IOException {
        Path hwmon0 = createHwmonDir(tempDir, 0, "coretemp");
        // temp1_input is 0 so it falls through to averaging temp2..temp6
        writeFile(hwmon0.resolve("temp1_input"), "0");
        writeFile(hwmon0.resolve("temp2_input"), "50000");
        writeFile(hwmon0.resolve("temp3_input"), "60000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // Average of 50000 and 60000 = 55000 millidegrees = 55.0 C
        assertThat(sensors.queryCpuTemperature(), closeTo(55.0, EPS));
    }

    @Test
    void testHwmonPrioritySelection(@TempDir Path tempDir) throws IOException {
        // hwmon0 has k10temp (priority 1), hwmon1 has coretemp (priority 0)
        Path hwmon0 = createHwmonDir(tempDir, 0, "k10temp");
        writeFile(hwmon0.resolve("temp1_input"), "70000");
        Path hwmon1 = createHwmonDir(tempDir, 1, "coretemp");
        writeFile(hwmon1.resolve("temp1_input"), "55000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // coretemp has higher priority (lower index), so 55.0 C
        assertThat(sensors.queryCpuTemperature(), closeTo(55.0, EPS));
    }

    @Test
    void testHwmonUnknownNameSkipped(@TempDir Path tempDir) throws IOException {
        // hwmon0 has an unknown sensor name — not in priority list, should be skipped for temp
        Path hwmon0 = createHwmonDir(tempDir, 0, "unknown_sensor");
        writeFile(hwmon0.resolve("temp1_input"), "60000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // No recognized temp sensor, falls through to thermal_zone (also absent) → 0
        assertThat(sensors.queryCpuTemperature(), closeTo(0d, EPS));
    }

    @Test
    void testHwmonTempPreventsThermalZoneFallback(@TempDir Path tempDir) throws IOException {
        // Both hwmon and thermal_zone have temp sensors; hwmon should win
        Path hwmon0 = createHwmonDir(tempDir, 0, "coretemp");
        writeFile(hwmon0.resolve("temp1_input"), "52000");
        Path tz0 = tempDir.resolve("thermal_zone0");
        Files.createDirectories(tz0);
        writeFile(tz0.resolve("type"), "x86_pkg_temp");
        writeFile(tz0.resolve("temp"), "99000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // hwmon coretemp found → thermal_zone never consulted → 52.0 C, not 99.0
        assertThat(sensors.queryCpuTemperature(), closeTo(52.0, EPS));
    }

    @Test
    void testThermalZoneFallback(@TempDir Path tempDir) throws IOException {
        // No hwmon temp sensor, so should fall back to thermal_zone
        Path tz0 = tempDir.resolve("thermal_zone0");
        Files.createDirectories(tz0);
        writeFile(tz0.resolve("type"), "x86_pkg_temp");
        writeFile(tz0.resolve("temp"), "48000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // 48000 millidegrees = 48.0 C
        assertThat(sensors.queryCpuTemperature(), closeTo(48.0, EPS));
    }

    @Test
    void testThermalZonePriority(@TempDir Path tempDir) throws IOException {
        // thermal_zone0 has x86_pkg_temp (priority 1), thermal_zone1 has cpu-thermal (priority 0)
        Path tz0 = tempDir.resolve("thermal_zone0");
        Files.createDirectories(tz0);
        writeFile(tz0.resolve("type"), "x86_pkg_temp");
        writeFile(tz0.resolve("temp"), "48000");

        Path tz1 = tempDir.resolve("thermal_zone1");
        Files.createDirectories(tz1);
        writeFile(tz1.resolve("type"), "cpu-thermal");
        writeFile(tz1.resolve("temp"), "45000");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // cpu-thermal has higher priority → 45.0 C
        assertThat(sensors.queryCpuTemperature(), closeTo(45.0, EPS));
    }

    @Test
    void testFanSpeeds(@TempDir Path tempDir) throws IOException {
        Path hwmon0 = createHwmonDir(tempDir, 0, "nct6775");
        writeFile(hwmon0.resolve("fan1_input"), "1200");
        writeFile(hwmon0.resolve("fan2_input"), "800");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        int[] fans = sensors.queryFanSpeeds();
        assertThat(fans.length, is(2));
        assertThat(fans[0], is(1200));
        assertThat(fans[1], is(800));
    }

    @Test
    void testVoltage(@TempDir Path tempDir) throws IOException {
        Path hwmon0 = createHwmonDir(tempDir, 0, "nct6775");
        writeFile(hwmon0.resolve("in1_input"), "1200");

        LinuxSensors sensors = new LinuxSensors(tempDir.resolve("hwmon").toString(),
                tempDir.resolve("thermal_zone").toString(), false);
        // 1200 millivolts = 1.2 V
        assertThat(sensors.queryCpuVoltage(), closeTo(1.2, EPS));
    }

    /**
     * Creates an hwmon directory with a name file.
     *
     * @param base  the base temp directory
     * @param index the hwmon index (0, 1, ...)
     * @param name  the sensor name to write to the name file
     * @return the created hwmon directory path
     * @throws IOException if file creation fails
     */
    private static Path createHwmonDir(Path base, int index, String name) throws IOException {
        Path hwmon = base.resolve("hwmon" + index);
        Files.createDirectories(hwmon);
        writeFile(hwmon.resolve("name"), name);
        return hwmon;
    }

    /**
     * Writes a string to a file.
     *
     * @param path    the file path
     * @param content the content to write
     * @throws IOException if writing fails
     */
    private static void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
