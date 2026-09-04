/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmHardware;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;

/**
 * Tests what each sensor read asks the hardware monitors for.
 * <p>
 * Every sensor is reached the same way: filter the {@code Hardware} class on {@code HardwareType} to find the
 * processor, then read that identifier's sensors of the wanted type. Voltage instead asked for
 * {@code SensorType="Voltage"} on a class that has no such property, so it could never match; these assertions pin the
 * shape of all three queries so that cannot recur.
 */
class WindowsSensorsQueryTest {

    @Test
    void testTemperatureQueriesTheProcessorsTemperatureSensors() {
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryCpuTemperature();
        assertThat("Both monitors queried for the processor's temperature", sensors.queries,
                contains("Hardware/CPU/Temperature", "Hardware/Cpu/Temperature"));
    }

    @Test
    void testFanSpeedQueriesTheProcessorsFanSensors() {
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryFanSpeeds();
        assertThat("Both monitors queried for the processor's fans", sensors.queries,
                contains("Hardware/CPU/Fan", "Hardware/Cpu/Fan"));
    }

    @Test
    void testVoltageQueriesTheProcessorsVoltageSensors() {
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryCpuVoltage();
        assertThat("Both monitors queried for the processor's voltage", sensors.queries,
                contains("Hardware/CPU/Voltage", "Hardware/Cpu/Voltage"));
    }

    @Test
    void testQueriesFilterTheHardwareClass() {
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryCpuTemperature();
        sensors.queryFanSpeeds();
        sensors.queryCpuVoltage();
        for (String query : sensors.queries) {
            String typeToQuery = query.substring(0, query.indexOf('/'));
            assertThat("A sensor query filters the Hardware class, which is the class carrying HardwareType",
                    typeToQuery, is(OhmHardware.HARDWARE));
        }
    }

    /** Records the query each tier makes as {@code typeToQuery/typeName/sensorType}, and returns no results. */
    private static final class RecordingSensors extends WindowsSensors {
        private final List<String> queries = new ArrayList<>();

        @Override
        protected @Nullable WmiResult<ValueProperty> queryHardwareMonitorSensor(String namespace, String typeToQuery,
                String typeName, String sensorType) {
            queries.add(typeToQuery + '/' + typeName + '/' + sensorType);
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
