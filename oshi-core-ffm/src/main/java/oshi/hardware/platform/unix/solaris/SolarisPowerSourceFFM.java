/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.MemorySegment;
import java.time.LocalDate;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.PowerSource;
import oshi.hardware.common.platform.unix.solaris.SolarisPowerSource;

/**
 * FFM-backed Solaris PowerSource.
 */
@ThreadSafe
public final class SolarisPowerSourceFFM extends SolarisPowerSource {

    private static final String[] KSTAT_BATT_MOD = { null, "battery", "acpi_drv" };

    private static final int KSTAT_BATT_IDX;

    static {
        int idx = 0;
        try (KstatChain kc = KstatUtilFFM.openChain()) {
            if (kc.lookup(KSTAT_BATT_MOD[1], 0, null).address() != 0L) {
                idx = 1;
            } else if (kc.lookup(KSTAT_BATT_MOD[2], 0, null).address() != 0L) {
                idx = 2;
            }
        }
        KSTAT_BATT_IDX = idx;
    }

    public SolarisPowerSourceFFM(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
        return buildPowerSources("BAT0", readBattery(), SolarisPowerSourceFFM::new);
    }

    private static BatteryReadings readBattery() {
        BatteryReadings readings = new BatteryReadings();
        if (KSTAT_BATT_IDX > 0) {
            try (KstatChain kc = KstatUtilFFM.openChain()) {
                MemorySegment ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BIF0");
                if (ksp.address() != 0L && kc.read(ksp)) {
                    readings.bifValid = true;
                    readings.bifLastCap = KstatUtilFFM.dataLookupLong(ksp, "bif_last_cap");
                    readings.bifDesignCap = KstatUtilFFM.dataLookupLong(ksp, "bif_design_cap");
                    readings.bifUnit = KstatUtilFFM.dataLookupLong(ksp, "bif_unit");
                    readings.bifModel = KstatUtilFFM.dataLookupString(ksp, "bif_model");
                    readings.bifSerial = KstatUtilFFM.dataLookupString(ksp, "bif_serial");
                    readings.bifType = KstatUtilFFM.dataLookupString(ksp, "bif_type");
                    readings.bifOemInfo = KstatUtilFFM.dataLookupString(ksp, "bif_oem_info");
                }

                ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BST0");
                if (ksp.address() != 0L && kc.read(ksp)) {
                    readings.bstValid = true;
                    readings.bstRemCap = KstatUtilFFM.dataLookupLong(ksp, "bst_rem_cap");
                    readings.bstRate = KstatUtilFFM.dataLookupLong(ksp, "bst_rate");
                    readings.bstState = KstatUtilFFM.dataLookupLong(ksp, "bst_state");
                    readings.bstVoltage = KstatUtilFFM.dataLookupLong(ksp, "bst_voltage");
                }
            }
        }
        return readings;
    }
}
