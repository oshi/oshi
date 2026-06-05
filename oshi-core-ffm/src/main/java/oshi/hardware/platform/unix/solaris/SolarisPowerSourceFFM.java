/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.solaris;

import java.lang.foreign.MemorySegment;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM;
import oshi.ffm.util.platform.unix.solaris.KstatUtilFFM.KstatChain;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;

@ThreadSafe
public final class SolarisPowerSourceFFM extends AbstractPowerSource {

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

    public static List<PowerSource> getPowerSources() {
        return Arrays.asList(getPowerSource("BAT0"));
    }

    private static SolarisPowerSourceFFM getPowerSource(String name) {
        String psName = name;
        String psDeviceName = Constants.UNKNOWN;
        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d;
        double psTimeRemainingInstant = 0d;
        double psPowerUsageRate = 0d;
        double psVoltage = -1d;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        CapacityUnits psCapacityUnits = CapacityUnits.RELATIVE;
        int psCurrentCapacity = 0;
        int psMaxCapacity = 1;
        int psDesignCapacity = 1;
        int psCycleCount = -1;
        String psChemistry = Constants.UNKNOWN;
        LocalDate psManufactureDate = null;
        String psManufacturer = Constants.UNKNOWN;
        String psSerialNumber = Constants.UNKNOWN;
        double psTemperature = 0d;

        if (KSTAT_BATT_IDX > 0) {
            try (KstatChain kc = KstatUtilFFM.openChain()) {
                MemorySegment ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BIF0");
                if (ksp.address() != 0L && kc.read(ksp)) {
                    long energyFull = KstatUtilFFM.dataLookupLong(ksp, "bif_last_cap");
                    if (energyFull == 0xffffffffL || energyFull <= 0) {
                        energyFull = KstatUtilFFM.dataLookupLong(ksp, "bif_design_cap");
                    }
                    if (energyFull != 0xffffffffL && energyFull > 0) {
                        psMaxCapacity = (int) energyFull;
                    }
                    long unit = KstatUtilFFM.dataLookupLong(ksp, "bif_unit");
                    if (unit == 0) {
                        psCapacityUnits = CapacityUnits.MWH;
                    } else if (unit == 1) {
                        psCapacityUnits = CapacityUnits.MAH;
                    }
                    psDeviceName = KstatUtilFFM.dataLookupString(ksp, "bif_model");
                    psSerialNumber = KstatUtilFFM.dataLookupString(ksp, "bif_serial");
                    psChemistry = KstatUtilFFM.dataLookupString(ksp, "bif_type");
                    psManufacturer = KstatUtilFFM.dataLookupString(ksp, "bif_oem_info");
                }

                ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BST0");
                if (ksp.address() != 0L && kc.read(ksp)) {
                    long energyNow = KstatUtilFFM.dataLookupLong(ksp, "bst_rem_cap");
                    if (energyNow >= 0) {
                        psCurrentCapacity = (int) energyNow;
                    }
                    long powerNow = KstatUtilFFM.dataLookupLong(ksp, "bst_rate");
                    if (powerNow == 0xFFFFFFFFL) {
                        powerNow = 0L;
                    }
                    psPowerUsageRate = powerNow;
                    // Battery State:
                    // bit 0 = discharging
                    // bit 1 = charging
                    // bit 2 = critical energy state
                    long bstState = KstatUtilFFM.dataLookupLong(ksp, "bst_state");
                    psDischarging = (bstState & 0x1) > 0;
                    psCharging = (bstState & 0x2) > 0;

                    if (psDischarging) {
                        psTimeRemainingEstimated = powerNow > 0 ? 3600d * energyNow / powerNow : -1d;
                    }

                    long voltageNow = KstatUtilFFM.dataLookupLong(ksp, "bst_voltage");
                    if (voltageNow > 0) {
                        psVoltage = voltageNow / 1000d;
                        psAmperage = psPowerUsageRate * 1000d / voltageNow;
                    }
                }
            }
        }

        return new SolarisPowerSourceFFM(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }
}
