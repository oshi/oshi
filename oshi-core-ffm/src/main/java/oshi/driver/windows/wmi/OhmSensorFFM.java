/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.OhmSensor;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query Open Hardware Monitor WMI data for Sensors using FFM.
 */
@ThreadSafe
public final class OhmSensorFFM extends OhmSensor {

    private OhmSensorFFM() {
    }

    /**
     * Queries the sensor value of a hardware identifier and sensor type.
     *
     * @param h          An instantiated {@link WmiQueryHandlerFFM}. User should have already initialized COM.
     * @param identifier The identifier whose value to query.
     * @param sensorType The type of sensor to query.
     * @return The sensor value.
     */
    public static WmiResult<ValueProperty> querySensorValue(WmiQueryHandlerFFM h, String identifier,
            String sensorType) {
        WmiQuery<ValueProperty> ohmSensorQuery = new WmiQuery<>(OHM_NAMESPACE,
                buildSensorWmiClassNameWithWhere(identifier, sensorType), ValueProperty.class);
        return h.queryWMI(ohmSensorQuery, false);
    }
}
