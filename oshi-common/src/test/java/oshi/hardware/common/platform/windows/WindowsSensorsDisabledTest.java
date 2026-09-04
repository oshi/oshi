/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import oshi.driver.common.windows.wmi.LhmSensor;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.OhmHardware;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.Win32Processor.VoltProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.GlobalConfig;

/**
 * Tests that the hardware monitor configuration switches suppress the sensor tiers which use them. The switches are
 * global state, so these tests must not run alongside any other test which reads or writes the configuration.
 */
@Isolated
class WindowsSensorsDisabledTest {

    private final @Nullable String ohmConfig = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED);
    private final @Nullable String lhmConfig = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED);

    @AfterEach
    void restoreConfig() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, ohmConfig);
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, lhmConfig);
    }

    @Test
    void testBothMonitorsQueriedByDefault() {
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryCpuTemperature();
        assertThat("Both namespaces queried for temperature", sensors.namespaces,
                contains(OhmHardware.OHM_NAMESPACE, LhmSensor.LHM_NAMESPACE));

        sensors.namespaces.clear();
        sensors.queryFanSpeeds();
        assertThat("Both namespaces queried for fan speed", sensors.namespaces,
                contains(OhmHardware.OHM_NAMESPACE, LhmSensor.LHM_NAMESPACE));

        sensors.namespaces.clear();
        sensors.queryCpuVoltage();
        assertThat("Both namespaces queried for voltage", sensors.namespaces,
                contains(OhmHardware.OHM_NAMESPACE, LhmSensor.LHM_NAMESPACE));
    }

    @Test
    void testDisablingOneMonitorLeavesTheOther() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, true);
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryCpuTemperature();
        sensors.queryFanSpeeds();
        sensors.queryCpuVoltage();
        assertThat("Only LHM queried", sensors.namespaces,
                contains(LhmSensor.LHM_NAMESPACE, LhmSensor.LHM_NAMESPACE, LhmSensor.LHM_NAMESPACE));

        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, false);
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, true);
        sensors.namespaces.clear();
        sensors.queryCpuTemperature();
        sensors.queryFanSpeeds();
        sensors.queryCpuVoltage();
        assertThat("Only OHM queried", sensors.namespaces,
                contains(OhmHardware.OHM_NAMESPACE, OhmHardware.OHM_NAMESPACE, OhmHardware.OHM_NAMESPACE));
    }

    @Test
    void testDisablingBothMonitorsSkipsAllQueries() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, true);
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, true);
        RecordingSensors sensors = new RecordingSensors();
        sensors.queryCpuTemperature();
        sensors.queryFanSpeeds();
        sensors.queryCpuVoltage();
        assertThat("No monitor queried", sensors.namespaces, is(empty()));
    }

    /** Records the namespaces queried and returns no results from any source. */
    private static final class RecordingSensors extends WindowsSensors {
        private final List<String> namespaces = new ArrayList<>();

        @Override
        protected @Nullable WmiResult<ValueProperty> queryHardwareMonitorSensor(String namespace, String typeToQuery,
                String typeName, String sensorType, boolean searchCpu) {
            namespaces.add(namespace);
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
