/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code MSAcpi_ThermalZoneTemperature} using FFM.
 */
@ThreadSafe
public final class MSAcpiThermalZoneTemperatureFFM extends MSAcpiThermalZoneTemperature {

    private MSAcpiThermalZoneTemperatureFFM() {
    }

    /**
     * Queries the current temperature.
     *
     * @return Temperature at thermal zone in tenths of degrees Kelvin.
     */
    public static WmiResult<TemperatureProperty> queryCurrentTemperature() {
        WmiQuery<TemperatureProperty> curTempQuery = new WmiQuery<>(WMI_NAMESPACE, MS_ACPI_THERMAL_ZONE_TEMPERATURE,
                TemperatureProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(curTempQuery);
    }
}
