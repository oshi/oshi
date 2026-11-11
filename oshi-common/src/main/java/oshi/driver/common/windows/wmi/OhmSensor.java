/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import java.util.Collections;
import java.util.List;

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
        /** Sensor name, such as {@code CPU Package} or {@code Core #1}. */
        NAME,
        /** Sensor value. */
        VALUE;
    }

    /**
     * Constructor.
     */
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
        return buildSensorWmiClassNameWithWhere(Collections.singletonList(identifier), sensorType);
    }

    /**
     * Builds the WMI class name with WHERE clause for sensor value queries across several hardware identifiers, such as
     * each of a board's Super I/O chips.
     *
     * @param identifiers The identifiers whose values to query, at least one
     * @param sensorType  The type of sensor to query
     * @return the WMI class name with WHERE clause
     */
    public static String buildSensorWmiClassNameWithWhere(List<String> identifiers, String sensorType) {
        StringBuilder sb = new StringBuilder(SENSOR).append(" WHERE ");
        if (identifiers.size() > 1) {
            sb.append('(');
        }
        for (int i = 0; i < identifiers.size(); i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            sb.append("Parent = \"").append(identifiers.get(i)).append('"');
        }
        if (identifiers.size() > 1) {
            sb.append(')');
        }
        sb.append(" AND SensorType = \"").append(sensorType).append('"');
        return sb.toString();
    }

    /**
     * Queries the sensor value of a hardware identifier and sensor type.
     *
     * @param h          An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @param namespace  the WMI namespace to query, either {@link #OHM_NAMESPACE} or {@link LhmSensor#LHM_NAMESPACE}
     * @param identifier The identifier whose value to query.
     * @param sensorType The type of sensor to query.
     * @return The sensor value.
     */
    public static WmiResult<ValueProperty> querySensorValue(WmiQueryExecutor h, String namespace, String identifier,
            String sensorType) {
        WmiQuery<ValueProperty> ohmSensorQuery = new WmiQuery<>(namespace,
                buildSensorWmiClassNameWithWhere(identifier, sensorType), ValueProperty.class);
        return h.queryWMI(ohmSensorQuery, false);
    }

    /**
     * Queries the sensor values of several hardware identifiers and one sensor type in a single query.
     *
     * @param h           An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @param namespace   the WMI namespace to query, either {@link #OHM_NAMESPACE} or {@link LhmSensor#LHM_NAMESPACE}
     * @param identifiers The identifiers whose values to query, at least one.
     * @param sensorType  The type of sensor to query.
     * @return The sensor values of every identifier.
     */
    public static WmiResult<ValueProperty> querySensorValue(WmiQueryExecutor h, String namespace,
            List<String> identifiers, String sensorType) {
        WmiQuery<ValueProperty> ohmSensorQuery = new WmiQuery<>(namespace,
                buildSensorWmiClassNameWithWhere(identifiers, sensorType), ValueProperty.class);
        return h.queryWMI(ohmSensorQuery, false);
    }
}
