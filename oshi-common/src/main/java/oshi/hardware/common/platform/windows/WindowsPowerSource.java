/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import java.time.LocalDate;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractPowerSource;

/**
 * A Power Source
 */
@ThreadSafe
public abstract class WindowsPowerSource extends AbstractPowerSource {

    // IOCTL codes
    protected static final int IOCTL_BATTERY_QUERY_TAG = 0x294040;
    protected static final int IOCTL_BATTERY_QUERY_INFORMATION = 0x294044;
    protected static final int IOCTL_BATTERY_QUERY_STATUS = 0x29404c;

    // Capability flags (BATTERY_INFORMATION.Capabilities / BATTERY_STATUS.PowerState)
    protected static final int BATTERY_SYSTEM_BATTERY = 0x80000000;
    protected static final int BATTERY_IS_SHORT_TERM = 0x20000000;
    protected static final int BATTERY_CAPACITY_RELATIVE = 0x40000000;
    protected static final int BATTERY_POWER_ON_LINE = 0x00000001;
    protected static final int BATTERY_DISCHARGING = 0x00000002;
    protected static final int BATTERY_CHARGING = 0x00000004;

    // BATTERY_QUERY_INFORMATION_LEVEL ordinals
    protected static final int BATTERY_INFORMATION_LEVEL = 0;
    protected static final int BATTERY_TEMPERATURE_LEVEL = 2;
    protected static final int BATTERY_ESTIMATED_TIME_LEVEL = 3;
    protected static final int BATTERY_DEVICE_NAME_LEVEL = 4;
    protected static final int BATTERY_MANUFACTURE_DATE_LEVEL = 5;
    protected static final int BATTERY_MANUFACTURE_NAME_LEVEL = 6;
    protected static final int BATTERY_SERIAL_NUMBER_LEVEL = 8;

    protected WindowsPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
            double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage,
            double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
            CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
            int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
            String psSerialNumber, double psTemperature) {
        super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
    }

}
