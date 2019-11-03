/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.unix.solaris;

import java.time.LocalDate;

import com.sun.jna.platform.unix.solaris.LibKstat.Kstat; // NOSONAR

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.platform.unix.solaris.KstatUtil;
import oshi.util.platform.unix.solaris.KstatUtil.KstatChain;

/**
 * A Power Source
 */
public class SolarisPowerSource extends AbstractPowerSource {

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


    public SolarisPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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

    /**
     * Gets Battery Information
     *
     * @return An array of PowerSource objects representing batteries, etc.
     */
    public static PowerSource[] getPowerSources() {
        SolarisPowerSource[] ps = new SolarisPowerSource[1];
        ps[0] = getPowerSource("BAT0");
        return ps;
    }

    private static SolarisPowerSource getPowerSource(String name) {
        String psName = Constants.UNKNOWN;
        String psDeviceName = Constants.UNKNOWN;
        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
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

        // If no kstat info, return empty
        if (KSTAT_BATT_IDX > 0) {
            // Get kstat for the battery information
            try (KstatChain kc = KstatUtil.openChain()) {
                Kstat ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BIF0");
                if (ksp != null) {
                    // Predicted battery capacity when fully charged.
                    long energyFull = KstatUtil.dataLookupLong(ksp, "bif_last_cap");
                    if (energyFull == 0xffffffff || energyFull <= 0) {
                        energyFull = KstatUtil.dataLookupLong(ksp, "bif_design_cap");
                    }
                    if (energyFull != 0xffffffff && energyFull > 0) {
                        psMaxCapacity = (int) energyFull;
                    }
                    // kstat_data_lookup(ksp, "bif_unit"); --> 0 = mWh, 1=mAh
                    // bif_model, bif_serial, bif_type, bif_oem_info
                    // all char[MAXNAMELEN]
                }

                // Get kstat for the battery state
                ksp = kc.lookup(KSTAT_BATT_MOD[KSTAT_BATT_IDX], 0, "battery BST0");
                if (ksp != null) {
                    // estimated remaining battery capacity
                    long energyNow = KstatUtil.dataLookupLong(ksp, "bst_rem_cap");
                    if (energyNow >= 0) {
                        psCurrentCapacity = (int) energyNow;
                    }
                    // power or current supplied at battery terminal
                    long powerNow = KstatUtil.dataLookupLong(ksp, "bst_rate");
                    if (powerNow == 0xFFFFFFFF) {
                        powerNow = 0L;
                    }
                    // Battery State:
                    // bit 0 = discharging
                    // bit 1 = charging
                    // bit 2 = critical energy state
                    boolean isCharging = (KstatUtil.dataLookupLong(ksp, "bst_state") & 0x10) > 0;

                    if (!isCharging) {
                        psTimeRemainingEstimated = powerNow > 0 ? 3600d * energyNow / powerNow : -1d;
                    }
                    // bst_voltage (mV0)
                }
            }
        }

        return new SolarisPowerSource(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }
}
