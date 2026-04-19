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
        CURRENTTEMPERATURE;
    }

    protected MSAcpiThermalZoneTemperature() {
    }
}
