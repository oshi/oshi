/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.OhmSensor;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query Open Hardware Monitor WMI data for Sensors using JNA.
 */
@ThreadSafe
public final class OhmSensorJNA extends OhmSensor {

    private OhmSensorJNA() {
    }

    /**
     * Queries the sensor value of a hardware identifier and sensor type.
     *
     * @param h          An instantiated {@link WmiQueryHandler}. User should have already initialized COM.
     * @param identifier The identifier whose value to query.
     * @param sensorType The type of sensor to query.
     * @return The sensor value.
     */
    public static WmiResult<ValueProperty> querySensorValue(WmiQueryHandler h, String identifier, String sensorType) {
        WmiQuery<ValueProperty> ohmSensorQuery = new WmiQuery<>(OHM_NAMESPACE,
                buildSensorWmiClassNameWithWhere(identifier, sensorType), ValueProperty.class);
        return h.queryWMI(ohmSensorQuery, false);
    }
}
