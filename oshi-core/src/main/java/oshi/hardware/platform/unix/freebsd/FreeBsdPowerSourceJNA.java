/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.time.LocalDate;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.unix.freebsd.FreeBsdPowerSource;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A Power Source
 */
@ThreadSafe
public final class FreeBsdPowerSourceJNA extends FreeBsdPowerSource {

    public FreeBsdPowerSourceJNA(String psName, String psDeviceName, double psRemainingCapacityPercent,
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

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
    }

    /**
     * Gets Battery Information.
     *
     * @return A list of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        return queryBattery("BAT0", BsdSysctlUtil::sysctl, FreeBsdPowerSourceJNA::new);
    }
}
