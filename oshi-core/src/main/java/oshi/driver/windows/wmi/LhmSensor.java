/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Utility to query LibreHardwareMonitor WMI sensor data for GPU metrics.
 *
 * <p>
 * LHM publishes sensor data to {@code ROOT\LibreHardwareMonitor} when it is running. This class queries the
 * {@code Sensor} table filtered by hardware parent identifier and sensor type.
 */
@ThreadSafe
public final class LhmSensor {

    private static final String SENSOR = "Sensor";

    /**
     * Sensor properties returned by LHM WMI queries.
     */
    public enum LhmSensorProperty {
        NAME, VALUE, PARENT;
    }

    private LhmSensor() {
    }

    /**
     * Queries all sensors of a given type belonging to a specific hardware parent.
     *
     * @param parent     the LHM hardware identifier (e.g. {@code /gpu-nvidia/0})
     * @param sensorType the sensor type string (e.g. {@code "Load"}, {@code "SmallData"})
     * @return WMI result containing NAME, VALUE, and PARENT columns
     */
    public static WmiResult<LhmSensorProperty> querySensors(String parent, String sensorType) {
        StringBuilder sb = new StringBuilder(SENSOR);
        sb.append(" WHERE Parent=\"").append(parent);
        sb.append("\" AND SensorType=\"").append(sensorType).append('"');
        WmiQuery<LhmSensorProperty> query = new WmiQuery<>(WmiUtil.LHM_NAMESPACE, sb.toString(),
                LhmSensorProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(query, true);
    }

    /**
     * Queries all GPU hardware entries from LHM to discover parent identifiers.
     *
     * @return WMI result with IDENTIFIER and NAME columns for all GPU hardware entries
     */
    public static WmiResult<LhmHardwareProperty> queryGpuHardware() {
        WmiQuery<LhmHardwareProperty> query = new WmiQuery<>(WmiUtil.LHM_NAMESPACE,
                "Hardware WHERE HardwareType=\"GpuNvidia\" OR HardwareType=\"GpuAmd\" OR HardwareType=\"GpuIntel\"",
                LhmHardwareProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(query, true);
    }

    /**
     * LHM Hardware properties.
     */
    public enum LhmHardwareProperty {
        IDENTIFIER, NAME;
    }
}
