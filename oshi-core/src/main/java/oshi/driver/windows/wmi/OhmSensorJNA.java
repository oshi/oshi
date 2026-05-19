/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.OhmSensor;
import oshi.driver.common.windows.wmi.OhmSensor.ValueProperty;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.driver.common.windows.wmi.WmiResult;

@ThreadSafe
public final class OhmSensorJNA extends OhmSensor {
    private OhmSensorJNA() {
    }

    public static WmiResult<ValueProperty> querySensorValue(WmiQueryExecutor h, String identifier, String sensorType) {
        return OhmSensor.querySensorValue(h, identifier, sensorType);
    }
}
