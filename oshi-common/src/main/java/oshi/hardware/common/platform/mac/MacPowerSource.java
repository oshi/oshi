/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import java.time.LocalDate;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.AbstractPowerSource;

/**
 * A Power Source
 */
@ThreadSafe
public abstract class MacPowerSource extends AbstractPowerSource {

    /**
     * Creates a MacPowerSource.
     *
     * @param psName                     power source name
     * @param psDeviceName               device name
     * @param psRemainingCapacityPercent remaining capacity percentage
     * @param psTimeRemainingEstimated   estimated time remaining
     * @param psTimeRemainingInstant     instant time remaining
     * @param psPowerUsageRate           power usage rate
     * @param psVoltage                  voltage
     * @param psAmperage                 amperage
     * @param psPowerOnLine              whether on AC power
     * @param psCharging                 whether charging
     * @param psDischarging              whether discharging
     * @param psCapacityUnits            capacity units
     * @param psCurrentCapacity          current capacity
     * @param psMaxCapacity              max capacity
     * @param psDesignCapacity           design capacity
     * @param psCycleCount               cycle count
     * @param psChemistry                chemistry
     * @param psManufactureDate          manufacture date
     * @param psManufacturer             manufacturer
     * @param psSerialNumber             serial number
     * @param psTemperature              temperature
     */
    public MacPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
