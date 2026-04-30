/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.LhmSensor;
import oshi.driver.common.windows.wmi.LhmSensor.LhmHardwareProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query LibreHardwareMonitor WMI sensor data using FFM.
 */
@ThreadSafe
public final class LhmSensorFFM extends LhmSensor {

    private LhmSensorFFM() {
    }

    /**
     * Queries all sensors of a given type belonging to a specific hardware parent.
     *
     * @param parent     the LHM hardware identifier (e.g. {@code /gpu-nvidia/0})
     * @param sensorType the sensor type string (e.g. {@code "Load"}, {@code "SmallData"})
     * @return WMI result containing NAME, VALUE, and PARENT columns
     */
    public static WmiResult<LhmSensorProperty> querySensors(String parent, String sensorType) {
        WmiQuery<LhmSensorProperty> query = new WmiQuery<>(LHM_NAMESPACE,
                buildSensorWmiClassNameWithWhere(parent, sensorType), LhmSensorProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(query, true);
    }

    /**
     * Queries all GPU hardware entries from LHM to discover parent identifiers.
     *
     * @return WMI result with IDENTIFIER and NAME columns for all GPU hardware entries
     */
    public static WmiResult<LhmHardwareProperty> queryGpuHardware() {
        WmiQuery<LhmHardwareProperty> query = new WmiQuery<>(LHM_NAMESPACE, buildGpuHardwareWmiClassName(),
                LhmHardwareProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(query, true);
    }
}
