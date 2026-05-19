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
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class LhmSensorFFM extends LhmSensor {
    private LhmSensorFFM() {
    }

    public static WmiResult<LhmSensorProperty> querySensors(String parent, String sensorType) {
        return LhmSensor.querySensors(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()), parent, sensorType);
    }

    public static WmiResult<LhmHardwareProperty> queryGpuHardware() {
        return LhmSensor.queryGpuHardware(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
