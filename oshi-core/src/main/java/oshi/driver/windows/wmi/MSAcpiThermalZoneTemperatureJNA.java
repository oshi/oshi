/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class MSAcpiThermalZoneTemperatureJNA extends MSAcpiThermalZoneTemperature {
    private MSAcpiThermalZoneTemperatureJNA() {
    }

    public static WmiResult<TemperatureProperty> queryCurrentTemperature() {
        return MSAcpiThermalZoneTemperature
                .queryCurrentTemperature(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
