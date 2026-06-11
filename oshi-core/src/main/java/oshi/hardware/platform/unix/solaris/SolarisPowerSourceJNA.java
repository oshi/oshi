/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.time.LocalDate;
import java.util.List;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.unix.solaris.SolarisPowerSource;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * JNA-backed Solaris PowerSource.
 */
@ThreadSafe
public final class SolarisPowerSourceJNA extends SolarisPowerSource {

    // One-time lookup to see which kstat module to use
    private static final String[] KSTAT_BATT_MOD = { null, "battery", "acpi_drv" };

    private static final int KSTAT_BATT_IDX;

    static {
        try (KstatChain kc = KstatUtil.openChain()) {
            if (kc.lookup(KSTAT_BATT_MOD[1], 0, null) != null) {
                KSTAT_BATT_IDX = 1;
            } else if (kc.lookup(KSTAT_BATT_MOD[2], 0, null) != null) {
                KSTAT_BATT_IDX = 2;
            } else {
                KSTAT_BATT_IDX = 0;
            }
        }
    }

    public SolarisPowerSourceJNA(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
        return buildPowerSources("BAT0", readBattery(), SolarisPowerSourceJNA::new);
    }

    private static BatteryReadings readBattery() {
        BatteryReadings readings = new BatteryReadings();
        // If no kstat info, return empty readings (all defaults)
        if (KSTAT_BATT_IDX > 0) {
            try (KstatChain kc = KstatUtil.openChain()) {
                Kstat ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BIF0");
                if (ksp != null && kc.read(ksp)) {
                    readings.bifValid = true;
                    readings.bifLastCap = KstatUtil.dataLookupLong(ksp, "bif_last_cap");
                    readings.bifDesignCap = KstatUtil.dataLookupLong(ksp, "bif_design_cap");
                    readings.bifUnit = KstatUtil.dataLookupLong(ksp, "bif_unit");
                    readings.bifModel = KstatUtil.dataLookupString(ksp, "bif_model");
                    readings.bifSerial = KstatUtil.dataLookupString(ksp, "bif_serial");
                    readings.bifType = KstatUtil.dataLookupString(ksp, "bif_type");
                    readings.bifOemInfo = KstatUtil.dataLookupString(ksp, "bif_oem_info");
                }

                ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BST0");
                if (ksp != null && kc.read(ksp)) {
                    readings.bstValid = true;
                    readings.bstRemCap = KstatUtil.dataLookupLong(ksp, "bst_rem_cap");
                    readings.bstRate = KstatUtil.dataLookupLong(ksp, "bst_rate");
                    readings.bstState = KstatUtil.dataLookupLong(ksp, "bst_state");
                    readings.bstVoltage = KstatUtil.dataLookupLong(ksp, "bst_voltage");
                }
            }
        }
        return readings;
    }
}
