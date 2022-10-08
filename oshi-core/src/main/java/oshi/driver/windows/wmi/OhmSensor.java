/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;

/**
 * Utility to query Open Hardware Monitor WMI data for Sensors
 */
@ThreadSafe
public final class OhmSensor {

    private static final String SENSOR = "Sensor";

    /**
     * Sensor value property
     */
    public enum ValueProperty {
        VALUE;
    }

    private OhmSensor() {
    }

    /**
     * Queries the sensor value of an hardware identifier and sensor type.
     *
     * @param h          An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @param identifier The identifier whose value to query.
     * @param sensorType The type of sensor to query.
     * @return The sensor value.
     */
    public static WmiResult<ValueProperty> querySensorValue(WmiQueryHandler h, String identifier, String sensorType) {
        StringBuilder sb = new StringBuilder(SENSOR);
        sb.append(" WHERE Parent = \"").append(identifier);
        sb.append("\" AND SensorType=\"").append(sensorType).append('\"');
        WmiQuery<ValueProperty> ohmSensorQuery = new WmiQuery<>(WmiUtil.OHM_NAMESPACE, sb.toString(),
                ValueProperty.class);
        return h.queryWMI(ohmSensorQuery, false);
    }
}
