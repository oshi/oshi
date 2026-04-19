/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants, property enum, and WHERE clause builder for Open Hardware Monitor WMI Sensor data.
 */
@ThreadSafe
public class OhmSensor {

    /**
     * The WMI namespace for Open Hardware Monitor.
     */
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

    /**
     * The WMI class name for sensors.
     */
    public static final String SENSOR = "Sensor";

    /**
     * Sensor value property.
     */
    public enum ValueProperty {
        VALUE;
    }

    protected OhmSensor() {
    }

    /**
     * Builds the WMI class name with WHERE clause for sensor value queries.
     *
     * @param identifier The identifier whose value to query
     * @param sensorType The type of sensor to query
     * @return the WMI class name with WHERE clause
     */
    public static String buildSensorWmiClassNameWithWhere(String identifier, String sensorType) {
        StringBuilder sb = new StringBuilder(SENSOR);
        sb.append(" WHERE Parent = \"").append(identifier);
        sb.append("\" AND SensorType = \"").append(sensorType).append('"');
        return sb.toString();
    }
}
