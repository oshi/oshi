/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature;
import oshi.driver.common.windows.wmi.MSAcpiThermalZoneTemperature.TemperatureProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code MSAcpi_ThermalZoneTemperature} using JNA.
 */
@ThreadSafe
public final class MSAcpiThermalZoneTemperatureJNA extends MSAcpiThermalZoneTemperature {

    private MSAcpiThermalZoneTemperatureJNA() {
    }

    /**
     * Queries the current temperature.
     *
     * @return Temperature at thermal zone in tenths of degrees Kelvin.
     */
    public static WmiResult<TemperatureProperty> queryCurrentTemperature() {
        WmiQuery<TemperatureProperty> curTempQuery = new WmiQuery<>(WMI_NAMESPACE, MS_ACPI_THERMAL_ZONE_TEMPERATURE,
                TemperatureProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(curTempQuery);
    }
}
