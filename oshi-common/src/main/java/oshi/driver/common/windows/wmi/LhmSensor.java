/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants, property enums, and WHERE clause builders for LibreHardwareMonitor WMI sensor data.
 *
 * <p>
 * LHM publishes sensor data to {@code ROOT\LibreHardwareMonitor} when it is running. This class provides the shared
 * constants and query builders for the {@code Sensor} and {@code Hardware} tables.
 */
@ThreadSafe
public class LhmSensor {

    /**
     * The WMI namespace for LibreHardwareMonitor.
     */
    public static final String LHM_NAMESPACE = "ROOT\\LibreHardwareMonitor";

    /**
     * The WMI class name for sensors.
     */
    public static final String SENSOR = "Sensor";

    /**
     * The WMI class name for hardware.
     */
    public static final String HARDWARE = "Hardware";

    /**
     * Sensor properties returned by LHM WMI queries.
     */
    public enum LhmSensorProperty {
        /** Sensor name. */
        NAME,
        /** Sensor value. */
        VALUE,
        /** Parent hardware identifier. */
        PARENT;
    }

    /**
     * LHM Hardware properties.
     */
    public enum LhmHardwareProperty {
        /** Hardware identifier. */
        IDENTIFIER,
        /** Hardware name. */
        NAME;
    }

    /**
     * Default constructor.
     */
    protected LhmSensor() {
    }

    /**
     * Builds the WMI class name with WHERE clause for sensor queries.
     *
     * @param parent     the LHM hardware identifier (e.g. {@code /gpu-nvidia/0})
     * @param sensorType the sensor type string (e.g. {@code "Load"}, {@code "SmallData"})
     * @return the WMI class name with WHERE clause
     */
    public static String buildSensorWmiClassNameWithWhere(String parent, String sensorType) {
        StringBuilder sb = new StringBuilder(SENSOR);
        sb.append(" WHERE Parent=\"").append(parent);
        sb.append("\" AND SensorType=\"").append(sensorType).append('"');
        return sb.toString();
    }

    /**
     * Builds the WMI class name with WHERE clause for GPU hardware queries.
     *
     * @return the WMI class name with WHERE clause filtering GPU hardware types
     */
    public static String buildGpuHardwareWmiClassName() {
        return HARDWARE + " WHERE HardwareType=\"GpuNvidia\" OR HardwareType=\"GpuAmd\" OR HardwareType=\"GpuIntel\"";
    }

    /**
     * Queries all sensors of a given type belonging to a specific hardware parent.
     *
     * @param h          An instantiated {@link WmiQueryExecutor}.
     * @param parent     the LHM hardware identifier (e.g. {@code /gpu-nvidia/0})
     * @param sensorType the sensor type string (e.g. {@code "Load"}, {@code "SmallData"})
     * @return WMI result containing NAME, VALUE, and PARENT columns
     */
    public static WmiResult<LhmSensorProperty> querySensors(WmiQueryExecutor h, String parent, String sensorType) {
        WmiQuery<LhmSensorProperty> query = new WmiQuery<>(LHM_NAMESPACE,
                buildSensorWmiClassNameWithWhere(parent, sensorType), LhmSensorProperty.class);
        return h.queryWMI(query);
    }

    /**
     * Queries all GPU hardware entries from LHM to discover parent identifiers.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return WMI result with IDENTIFIER and NAME columns for all GPU hardware entries
     */
    public static WmiResult<LhmHardwareProperty> queryGpuHardware(WmiQueryExecutor h) {
        WmiQuery<LhmHardwareProperty> query = new WmiQuery<>(LHM_NAMESPACE, buildGpuHardwareWmiClassName(),
                LhmHardwareProperty.class);
        return h.queryWMI(query);
    }
}
