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

    /** IOCTL code to query battery tag. */
    protected static final int IOCTL_BATTERY_QUERY_TAG = 0x294040;
    /** IOCTL code to query battery information. */
    protected static final int IOCTL_BATTERY_QUERY_INFORMATION = 0x294044;
    /** IOCTL code to query battery status. */
    protected static final int IOCTL_BATTERY_QUERY_STATUS = 0x29404c;

    /** Battery capability flag: system battery. */
    protected static final int BATTERY_SYSTEM_BATTERY = 0x80000000;
    /** Battery capability flag: short-term (UPS). */
    protected static final int BATTERY_IS_SHORT_TERM = 0x20000000;
    /** Battery capability flag: capacity is relative (percent). */
    protected static final int BATTERY_CAPACITY_RELATIVE = 0x40000000;
    /** Battery power state flag: AC power online. */
    protected static final int BATTERY_POWER_ON_LINE = 0x00000001;
    /** Battery power state flag: discharging. */
    protected static final int BATTERY_DISCHARGING = 0x00000002;
    /** Battery power state flag: charging. */
    protected static final int BATTERY_CHARGING = 0x00000004;

    /** Query information level: battery information. */
    protected static final int BATTERY_INFORMATION_LEVEL = 0;
    /** Query information level: temperature. */
    protected static final int BATTERY_TEMPERATURE_LEVEL = 2;
    /** Query information level: estimated time remaining. */
    protected static final int BATTERY_ESTIMATED_TIME_LEVEL = 3;
    /** Query information level: device name. */
    protected static final int BATTERY_DEVICE_NAME_LEVEL = 4;
    /** Query information level: manufacture date. */
    protected static final int BATTERY_MANUFACTURE_DATE_LEVEL = 5;
    /** Query information level: manufacturer name. */
    protected static final int BATTERY_MANUFACTURE_NAME_LEVEL = 6;
    /** Query information level: serial number. */
    protected static final int BATTERY_SERIAL_NUMBER_LEVEL = 8;

    /**
     * Constructor.
     *
     * @param psName                     the psName
     * @param psDeviceName               the psDeviceName
     * @param psRemainingCapacityPercent the psRemainingCapacityPercent
     * @param psTimeRemainingEstimated   the psTimeRemainingEstimated
     * @param psTimeRemainingInstant     the psTimeRemainingInstant
     * @param psPowerUsageRate           the psPowerUsageRate
     * @param psVoltage                  the psVoltage
     * @param psAmperage                 the psAmperage
     * @param psPowerOnLine              the psPowerOnLine
     * @param psCharging                 the psCharging
     * @param psDischarging              the psDischarging
     * @param psCapacityUnits            the psCapacityUnits
     * @param psCurrentCapacity          the psCurrentCapacity
     * @param psMaxCapacity              the psMaxCapacity
     * @param psDesignCapacity           the psDesignCapacity
     * @param psCycleCount               the psCycleCount
     * @param psChemistry                the psChemistry
     * @param psManufactureDate          the psManufactureDate
     * @param psManufacturer             the psManufacturer
     * @param psSerialNumber             the psSerialNumber
     * @param psTemperature              the psTemperature
     */
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
