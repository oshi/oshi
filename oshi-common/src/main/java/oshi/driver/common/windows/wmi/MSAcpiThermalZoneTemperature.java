/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code MSAcpi_ThermalZoneTemperature}.
 */
@ThreadSafe
public class MSAcpiThermalZoneTemperature {

    /**
     * The WMI namespace for this class.
     */
    public static final String WMI_NAMESPACE = "ROOT\\WMI";

    /**
     * The WMI class name.
     */
    public static final String MS_ACPI_THERMAL_ZONE_TEMPERATURE = "MSAcpi_ThermalZoneTemperature";

    /**
     * Current temperature property.
     */
    public enum TemperatureProperty {
        /** Current temperature in tenths of degrees Kelvin. */
        CURRENTTEMPERATURE;
    }

    /**
     * Default constructor.
     */
    protected MSAcpiThermalZoneTemperature() {
    }

    /**
     * Queries current temperature from thermal zone.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Temperature information.
     */
    public static WmiResult<TemperatureProperty> queryCurrentTemperature(WmiQueryExecutor h) {
        WmiQuery<TemperatureProperty> query = new WmiQuery<>(WMI_NAMESPACE, MS_ACPI_THERMAL_ZONE_TEMPERATURE,
                TemperatureProperty.class);
        return h.queryWMI(query);
    }
}
