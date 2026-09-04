/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import oshi.driver.common.windows.wmi.LhmSensor.LhmHardwareProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.util.GlobalConfig;

/**
 * Tests the configuration switches which suppress hardware monitor queries. The switches are global state, so these
 * tests must not run alongside any other test which reads or writes the configuration.
 */
@Isolated
class HardwareMonitorDisabledTest {

    private static final String OTHER_NAMESPACE = "ROOT\\CIMV2";

    private final @Nullable String ohmConfig = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED);
    private final @Nullable String lhmConfig = GlobalConfig.get(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED);

    @AfterEach
    void restoreConfig() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, ohmConfig);
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, lhmConfig);
    }

    @Test
    void testEnabledByDefault() {
        assertThat("OHM enabled by default", HardwareMonitorDisabled.isWmiDisabled(OhmHardware.OHM_NAMESPACE),
                is(false));
        assertThat("LHM enabled by default", HardwareMonitorDisabled.isWmiDisabled(LhmSensor.LHM_NAMESPACE), is(false));
    }

    @Test
    void testSwitchesAreIndependent() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, true);
        assertThat("OHM disabled", HardwareMonitorDisabled.isWmiDisabled(OhmHardware.OHM_NAMESPACE), is(true));
        assertThat("LHM unaffected", HardwareMonitorDisabled.isWmiDisabled(LhmSensor.LHM_NAMESPACE), is(false));

        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, false);
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, true);
        assertThat("LHM disabled", HardwareMonitorDisabled.isWmiDisabled(LhmSensor.LHM_NAMESPACE), is(true));
        assertThat("OHM unaffected", HardwareMonitorDisabled.isWmiDisabled(OhmHardware.OHM_NAMESPACE), is(false));
    }

    @Test
    void testOtherNamespaceUnaffected() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_OHM_DISABLED, true);
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, true);
        assertThat("Unrelated namespace still queried", HardwareMonitorDisabled.isWmiDisabled(OTHER_NAMESPACE),
                is(false));
    }

    @Test
    void testDisabledLhmSkipsQueries() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, true);
        CountingExecutor executor = new CountingExecutor();

        WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(executor, "/gpu-nvidia/0", "Load");
        assertThat("No sensor rows", sensors.getResultCount(), is(0));
        WmiResult<LhmHardwareProperty> hardware = LhmSensor.queryGpuHardware(executor);
        assertThat("No hardware rows", hardware.getResultCount(), is(0));
        assertThat("WMI not queried", executor.queries, is(0));
    }

    @Test
    void testEnabledLhmQueries() {
        GlobalConfig.set(GlobalConfig.OSHI_OS_WINDOWS_LHM_DISABLED, false);
        CountingExecutor executor = new CountingExecutor();

        LhmSensor.querySensors(executor, "/gpu-nvidia/0", "Load");
        LhmSensor.queryGpuHardware(executor);
        assertThat("WMI queried once per call", executor.queries, is(2));
    }

    /** A {@link WmiQueryExecutor} which counts queries and returns no rows. */
    private static final class CountingExecutor implements WmiQueryExecutor {
        private int queries;

        @Override
        public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {
            queries++;
            return WmiResult.empty();
        }

        @Override
        public <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query, boolean initCom) {
            return queryWMI(query);
        }
    }
}
