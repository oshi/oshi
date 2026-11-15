/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.LhmMetrics;
import oshi.driver.common.windows.LhmMetrics.Reading;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Tests the sensor values taken from the web server of LibreHardwareMonitor 0.9.5 and later.
 */
class WindowsSensorsLhmMetricsTest {

    private static final List<Reading> READINGS = List.of(
            new Reading("lhm_cpu_load_percent", "/intelcpu/0", "CPU Total", 15.2),
            new Reading(LhmMetrics.CPU_TEMPERATURE, "/intelcpu/0", "CPU Package", 52.0),
            new Reading(LhmMetrics.CPU_TEMPERATURE, "/intelcpu/0", "Core #1", 48.0),
            new Reading(LhmMetrics.CPU_TEMPERATURE, "/intelcpu/0", "Core Max", 60.0),
            new Reading(LhmMetrics.CPU_TEMPERATURE, "/intelcpu/0", "Core #1 Distance to TjMax", 52.0),
            new Reading(LhmMetrics.CPU_TEMPERATURE, "/intelcpu/1", "CPU Package", 90.0),
            new Reading("lhm_storage_temperature_celsius", "/nvme/0", "Composite Temperature", 37.5),
            new Reading(LhmMetrics.MOTHERBOARD_FAN, "/lpc/nct6798d", "Fan 1", 0.0),
            new Reading(LhmMetrics.MOTHERBOARD_FAN, "/lpc/nct6798d", "Fan 2", 1123.0),
            new Reading(LhmMetrics.MOTHERBOARD_FAN, "/lpc/it8689e", "Fan 1", 850.0),
            new Reading("lhm_gpunvidia_fan_rpm", "/gpu-nvidia/0", "GPU Fan 1", 1500.0),
            new Reading(LhmMetrics.MOTHERBOARD_VOLTAGE, "/lpc/nct6798d", "+3.3V", 3.3),
            new Reading(LhmMetrics.MOTHERBOARD_VOLTAGE, "/lpc/nct6798d", "Vcore", 1.2));

    @Test
    void testCpuTemperatureAveragesTheFirstProcessorsReadings() {
        assertThat("Summaries, margins and the second socket are left out", WindowsSensors.getCpuTemperature(READINGS),
                closeTo(50.0, 1e-9));
        assertThat(WindowsSensors.getCpuTemperature(List.of()), is(0d));
    }

    @Test
    void testFanSpeedsAreTheMotherboardsFans() {
        assertThat("Every Super I/O chip's fans, an idle header included; not the GPU's",
                WindowsSensors.getFanSpeeds(READINGS), is(new int[] { 0, 1123, 850 }));
        assertThat(WindowsSensors.getFanSpeeds(List.of()), is(new int[0]));
    }

    @Test
    void testCpuVoltagePrefersTheProcessorsOwnSensor() {
        assertThat("Without a processor voltage sensor, the Super I/O Vcore", WindowsSensors.getCpuVoltage(READINGS),
                closeTo(1.2, 1e-9));
        List<Reading> withCpu = List.of(new Reading(LhmMetrics.MOTHERBOARD_VOLTAGE, "/lpc/nct6798d", "Vcore", 1.2),
                new Reading(LhmMetrics.CPU_VOLTAGE, "/amdcpu/0", "Core (SVI2 TFN)", 1.35));
        assertThat(WindowsSensors.getCpuVoltage(withCpu), closeTo(1.35, 1e-9));
        assertThat(WindowsSensors.getCpuVoltage(List.of()), is(0d));
    }

    @Test
    void testWebServerIsReadAfterBothWmiNamespaces() {
        StubSensors sensors = new StubSensors(READINGS);
        assertThat(sensors.queryCpuTemperature(), closeTo(50.0, 1e-9));
        assertThat(sensors.queryFanSpeeds(), is(new int[] { 0, 1123, 850 }));
        assertThat(sensors.queryCpuVoltage(), closeTo(1.2, 1e-9));
        assertThat("One request serves the three reads of a refresh", sensors.requests, is(1));
    }

    @Test
    void testNoServerConfiguredMakesNoRequest() {
        StubSensors sensors = new StubSensors(READINGS) {
            @Override
            protected List<Reading> queryLhmMetrics() {
                return unstubbedQuery();
            }
        };
        assertThat(sensors.queryCpuTemperature(), is(0d));
        assertThat(sensors.queryFanSpeeds(), is(new int[0]));
        assertThat(sensors.queryCpuVoltage(), is(0d));
    }

    /** Neither WMI namespace answers; the web server returns the given readings, counting requests. */
    private static class StubSensors extends WindowsSensors {
        private final List<Reading> readings;
        private int requests;

        StubSensors(List<Reading> readings) {
            this.readings = readings;
        }

        @Override
        protected List<Reading> queryLhmMetrics() {
            requests++;
            return readings;
        }

        /** The real query, which reads an unset address and so returns nothing. */
        List<Reading> unstubbedQuery() {
            return super.queryLhmMetrics();
        }

        @Override
        protected @Nullable WmiResult<ValueProperty> queryHardwareMonitorSensor(String namespace, String typeToQuery,
                String typeName, String sensorType) {
            return null;
        }

        @Override
        protected WmiResult<TemperatureProperty> queryWmiTemperature() {
            return WmiResult.empty();
        }

        @Override
        protected WmiResult<SpeedProperty> queryWmiFanSpeed() {
            return WmiResult.empty();
        }

        @Override
        protected WmiResult<VoltProperty> queryWmiVoltage() {
            return WmiResult.empty();
        }
    }
}
