/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.LhmSensor;
import oshi.driver.common.windows.wmi.LhmSensor.LhmHardwareProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query LibreHardwareMonitor WMI sensor data using JNA.
 */
@ThreadSafe
public final class LhmSensorJNA extends LhmSensor {

    private LhmSensorJNA() {
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
        WmiQueryHandler handler = WmiQueryHandler.createInstance();
        Objects.requireNonNull(handler, "WmiQueryHandler.createInstance() returned null");
        return handler.queryWMI(query, true);
    }

    /**
     * Queries all GPU hardware entries from LHM to discover parent identifiers.
     *
     * @return WMI result with IDENTIFIER and NAME columns for all GPU hardware entries
     */
    public static WmiResult<LhmHardwareProperty> queryGpuHardware() {
        WmiQuery<LhmHardwareProperty> query = new WmiQuery<>(LHM_NAMESPACE, buildGpuHardwareWmiClassName(),
                LhmHardwareProperty.class);
        WmiQueryHandler handler = WmiQueryHandler.createInstance();
        Objects.requireNonNull(handler, "WmiQueryHandler.createInstance() returned null");
        return handler.queryWMI(query, true);
    }
}
