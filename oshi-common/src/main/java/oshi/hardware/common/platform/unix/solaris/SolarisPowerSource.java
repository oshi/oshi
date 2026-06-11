/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.solaris;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;

/**
 * Abstract base for the Solaris PowerSource. The default values and the {@code bif}/{@code bst} battery math are
 * shared; the native {@code battery}/{@code acpi_drv} kstat read is done by the JNA and FFM subclasses, which populate
 * a {@link BatteryReadings} and hand it to {@link #buildPowerSources}.
 */
@ThreadSafe
public abstract class SolarisPowerSource extends AbstractPowerSource {

    /** The 32-bit "unknown" sentinel returned by the battery kstats, read as an unsigned long. */
    private static final long UNKNOWN_CAP = 0xFFFFFFFFL;

    /**
     * Raw values read from the {@code battery BIF0} (info) and {@code battery BST0} (state) kstats. Each block has a
     * validity flag set only when its kstat was read successfully.
     */
    public static final class BatteryReadings {
        public boolean bifValid;
        public long bifLastCap;
        public long bifDesignCap;
        public long bifUnit;
        public String bifModel = Constants.UNKNOWN;
        public String bifSerial = Constants.UNKNOWN;
        public String bifType = Constants.UNKNOWN;
        public String bifOemInfo = Constants.UNKNOWN;

        public boolean bstValid;
        public long bstRemCap;
        public long bstRate;
        public long bstState;
        public long bstVoltage;
    }

    /** Creates a concrete (JNA or FFM) Solaris power source from the computed battery fields. */
    @FunctionalInterface
    protected interface PowerSourceFactory {
        SolarisPowerSource create(String psName, String psDeviceName, double psRemainingCapacityPercent,
                double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate,
                double psVoltage, double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
                CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
                int psCycleCount, String psChemistry, LocalDate psManufactureDate, String psManufacturer,
                String psSerialNumber, double psTemperature);
    }

    protected SolarisPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
     * Computes a single battery {@link PowerSource} from the given readings, building the concrete instance with the
     * supplied factory.
     *
     * @param name     the power source name
     * @param readings the raw battery kstat values
     * @param factory  creates the platform-specific instance
     * @return a single-element list with the battery power source
     */
    protected static List<PowerSource> buildPowerSources(String name, BatteryReadings readings,
            PowerSourceFactory factory) {
        String psName = name;
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

        if (readings.bifValid) {
            // Predicted battery capacity when fully charged.
            long energyFull = readings.bifLastCap;
            if (energyFull == UNKNOWN_CAP || energyFull <= 0) {
                energyFull = readings.bifDesignCap;
            }
            if (energyFull != UNKNOWN_CAP && energyFull > 0) {
                psMaxCapacity = (int) energyFull;
            }
            if (readings.bifUnit == 0) {
                psCapacityUnits = CapacityUnits.MWH;
            } else if (readings.bifUnit == 1) {
                psCapacityUnits = CapacityUnits.MAH;
            }
            psDeviceName = readings.bifModel;
            psSerialNumber = readings.bifSerial;
            psChemistry = readings.bifType;
            psManufacturer = readings.bifOemInfo;
        }

        if (readings.bstValid) {
            // estimated remaining battery capacity
            long energyNow = readings.bstRemCap;
            if (energyNow >= 0) {
                psCurrentCapacity = (int) energyNow;
            }
            // power or current supplied at battery terminal
            long powerNow = readings.bstRate;
            if (powerNow == UNKNOWN_CAP) {
                powerNow = 0L;
            }
            psPowerUsageRate = powerNow;
            // Battery State:
            // bit 0 = discharging
            // bit 1 = charging
            // bit 2 = critical energy state
            long bstState = readings.bstState;
            psDischarging = (bstState & 0x1) > 0;
            psCharging = (bstState & 0x2) > 0;

            if (psDischarging) {
                psTimeRemainingEstimated = powerNow > 0 ? 3600d * energyNow / powerNow : -1d;
            }

            long voltageNow = readings.bstVoltage;
            if (voltageNow > 0) {
                psVoltage = voltageNow / 1000d;
                psAmperage = psPowerUsageRate * 1000d / voltageNow;
            }
        }

        return Arrays.asList(factory.create(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature));
    }
}
