/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.freebsd;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Shared FreeBSD power-source logic. Subclasses provide the sysctl backend (JNA or FFM) via
 * {@link #queryBatteries(BiFunction, PowerSourceFactory)}.
 */
public abstract class FreeBsdPowerSource extends AbstractPowerSource {

    /**
     * Constructs a new {@code FreeBsdPowerSource}.
     *
     * @param psName                     the power source name
     * @param psDeviceName               the power source device name
     * @param psRemainingCapacityPercent the remaining capacity as a fraction of the maximum
     * @param psTimeRemainingEstimated   the estimated time remaining in seconds
     * @param psTimeRemainingInstant     the reported instantaneous time remaining in seconds
     * @param psPowerUsageRate           the power usage rate in milliwatts
     * @param psVoltage                  the voltage in volts
     * @param psAmperage                 the amperage in milliamperes
     * @param psPowerOnLine              whether external power is connected
     * @param psCharging                 whether the power source is charging
     * @param psDischarging              whether the power source is discharging
     * @param psCapacityUnits            the units of the capacity values
     * @param psCurrentCapacity          the current capacity
     * @param psMaxCapacity              the maximum capacity
     * @param psDesignCapacity           the design capacity
     * @param psCycleCount               the charge cycle count
     * @param psChemistry                the battery chemistry
     * @param psManufactureDate          the manufacture date
     * @param psManufacturer             the manufacturer
     * @param psSerialNumber             the serial number
     * @param psTemperature              the temperature in degrees Celsius
     */
    protected FreeBsdPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
            double psTimeRemainingEstimated, double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage,
            double psAmperage, boolean psPowerOnLine, boolean psCharging, boolean psDischarging,
            CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity,
            int psCycleCount, String psChemistry, @Nullable LocalDate psManufactureDate, String psManufacturer,
            String psSerialNumber, double psTemperature) {
        super(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated, psTimeRemainingInstant,
                psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits,
                psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount, psChemistry, psManufactureDate,
                psManufacturer, psSerialNumber, psTemperature);
    }

    /**
     * Factory for creating platform-specific FreeBsdPowerSource instances.
     *
     * @param <T> the concrete power source type
     */
    @FunctionalInterface
    protected interface PowerSourceFactory<T extends FreeBsdPowerSource> {
        /**
         * Creates a platform-specific instance.
         *
         * @param psName                     the power source name
         * @param psDeviceName               the power source device name
         * @param psRemainingCapacityPercent the remaining capacity as a fraction of the maximum
         * @param psTimeRemainingEstimated   the estimated time remaining in seconds
         * @param psTimeRemainingInstant     the reported instantaneous time remaining in seconds
         * @param psPowerUsageRate           the power usage rate in milliwatts
         * @param psVoltage                  the voltage in volts
         * @param psAmperage                 the amperage in milliamperes
         * @param psPowerOnLine              whether external power is connected
         * @param psCharging                 whether the power source is charging
         * @param psDischarging              whether the power source is discharging
         * @param psCapacityUnits            the units of the capacity values
         * @param psCurrentCapacity          the current capacity
         * @param psMaxCapacity              the maximum capacity
         * @param psDesignCapacity           the design capacity
         * @param psCycleCount               the charge cycle count
         * @param psChemistry                the battery chemistry
         * @param psManufactureDate          the manufacture date
         * @param psManufacturer             the manufacturer
         * @param psSerialNumber             the serial number
         * @param psTemperature              the temperature in degrees Celsius
         * @return the new instance
         */
        T create(String psName, String psDeviceName, double psRemainingCapacityPercent, double psTimeRemainingEstimated,
                double psTimeRemainingInstant, double psPowerUsageRate, double psVoltage, double psAmperage,
                boolean psPowerOnLine, boolean psCharging, boolean psDischarging, CapacityUnits psCapacityUnits,
                int psCurrentCapacity, int psMaxCapacity, int psDesignCapacity, int psCycleCount, String psChemistry,
                @Nullable LocalDate psManufactureDate, String psManufacturer, String psSerialNumber,
                double psTemperature);
    }

    // hw.acpi.battery.state bits, from sys/dev/acpica/acpiio.h
    private static final int ACPI_BATT_STAT_DISCHARG = 1;
    private static final int ACPI_BATT_STAT_CHARGING = 2;

    /**
     * Queries every ACPI battery and constructs a power source for each one present, using the provided sysctl accessor
     * and factory.
     * <p>
     * The kernel creates the {@code hw.acpi.battery} sysctls only when a battery driver attaches, so their absence
     * means there is no battery. Each battery is read with {@code acpiconf -i}. The {@code hw.acpi.battery} values are
     * totals across all batteries, so they fill in for {@code acpiconf} only when there is a single battery.
     *
     * @param <T>       the concrete power source type
     * @param sysctlInt reads an integer sysctl given (name, default)
     * @param factory   constructs the platform-specific instance
     * @return a list of the batteries present, empty if there are none
     */
    protected static <T extends FreeBsdPowerSource> List<PowerSource> queryBatteries(
            BiFunction<String, Integer, Integer> sysctlInt, PowerSourceFactory<T> factory) {
        int units = sysctlInt.apply("hw.acpi.battery.units", 0);
        if (units <= 0) {
            return Collections.emptyList();
        }
        boolean acline = sysctlInt.apply("hw.acpi.acline", 0) == 1;
        int state = -1;
        int time = -1;
        int life = -1;
        if (units == 1) {
            state = sysctlInt.apply("hw.acpi.battery.state", -1);
            time = sysctlInt.apply("hw.acpi.battery.time", -1);
            life = sysctlInt.apply("hw.acpi.battery.life", -1);
        }
        List<PowerSource> batteries = new ArrayList<>();
        for (int unit = 0; unit < units; unit++) {
            T battery = parseBattery("BAT" + unit, ExecutingCommand.runNative("acpiconf -i " + unit), acline, state,
                    time, life, factory);
            if (battery != null) {
                batteries.add(battery);
            }
        }
        return Collections.unmodifiableList(batteries);
    }

    /**
     * Builds a power source from the output of {@code acpiconf -i}, falling back to the {@code hw.acpi.battery} totals
     * for values it does not print.
     *
     * @param <T>      the concrete power source type
     * @param name     the battery name
     * @param acpiconf the output of {@code acpiconf -i}, empty if it failed
     * @param acline   whether external power is connected
     * @param state    the {@code hw.acpi.battery.state} bits, or -1 if unknown
     * @param time     the {@code hw.acpi.battery.time} minutes remaining, or -1 if unknown
     * @param life     the {@code hw.acpi.battery.life} percent remaining, or -1 if unknown
     * @param factory  constructs the platform-specific instance
     * @return the power source, or {@code null} if {@code acpiconf} reports the battery not present
     */
    static <T extends FreeBsdPowerSource> @Nullable T parseBattery(String name, List<String> acpiconf, boolean acline,
            int state, int time, int life, PowerSourceFactory<T> factory) {
        Map<String, String> psMap = new HashMap<>();
        for (String line : acpiconf) {
            String[] split = line.split(":", 2);
            if (split.length > 1) {
                String value = split[1].trim();
                if (!value.isEmpty() && !"unknown".equals(value)) {
                    psMap.put(split[0].trim(), value);
                }
            }
        }

        boolean charging = (state & ACPI_BATT_STAT_CHARGING) != 0 && state > 0;
        boolean discharging = (state & ACPI_BATT_STAT_DISCHARG) != 0 && state > 0;
        String batteryState = psMap.get("State");
        if (batteryState != null) {
            if ("not present".equals(batteryState)) {
                return null;
            }
            discharging = batteryState.contains("discharging");
            charging = !discharging && batteryState.contains("charging");
        }

        double remainingCapacityPercent = 1d;
        String percent = psMap.get("Remaining capacity");
        if (percent != null) {
            remainingCapacityPercent = ParseUtil.getFirstIntValue(percent) / 100d;
        } else if (life >= 0) {
            remainingCapacityPercent = life / 100d;
        }

        double timeRemainingInstant = -1d;
        String remaining = psMap.get("Remaining time");
        if (remaining != null) {
            String[] hhmm = remaining.split(":", -1);
            if (hhmm.length == 2) {
                timeRemainingInstant = 3600d * ParseUtil.parseIntOrDefault(hhmm[0], 0)
                        + 60d * ParseUtil.parseIntOrDefault(hhmm[1], 0);
            }
        }
        double timeRemainingEstimated = time >= 0 ? 60d * time : timeRemainingInstant;
        if (timeRemainingInstant < 0) {
            timeRemainingInstant = timeRemainingEstimated;
        }

        // "N mW", or "N mA (M mW)" for a battery reporting in mA, or "N mA" when the voltage is unknown
        double powerUsageRate = 0d;
        double amperage = 0d;
        String rate = psMap.get("Present rate");
        if (rate != null) {
            if (rate.contains("mA")) {
                amperage = ParseUtil.getFirstIntValue(rate);
                int paren = rate.indexOf('(');
                if (paren >= 0) {
                    powerUsageRate = ParseUtil.getFirstIntValue(rate.substring(paren));
                }
            } else {
                powerUsageRate = ParseUtil.getFirstIntValue(rate);
            }
        }
        double voltage = -1d;
        String volts = psMap.get("Present voltage");
        if (volts != null) {
            int millivolts = ParseUtil.getFirstIntValue(volts);
            if (millivolts > 0) {
                voltage = millivolts / 1000d;
                if (amperage == 0d) {
                    // mW divided by V is mA
                    amperage = powerUsageRate / voltage;
                }
            }
        }
        // acpiconf prints magnitudes; the API reports discharge as negative
        if (discharging) {
            powerUsageRate = -powerUsageRate;
            amperage = -amperage;
        }

        CapacityUnits capacityUnits = CapacityUnits.RELATIVE;
        int designCapacity = 1;
        int maxCapacity = 1;
        String design = psMap.get("Design capacity");
        String lastFull = psMap.get("Last full capacity");
        if (design != null) {
            String units = design.toLowerCase(Locale.ROOT);
            if (units.endsWith("mah")) {
                capacityUnits = CapacityUnits.MAH;
            } else if (units.endsWith("mwh")) {
                capacityUnits = CapacityUnits.MWH;
            }
            if (capacityUnits != CapacityUnits.RELATIVE) {
                designCapacity = ParseUtil.getFirstIntValue(design);
                maxCapacity = lastFull == null ? designCapacity : ParseUtil.getFirstIntValue(lastFull);
            }
        }
        // acpiconf prints the remaining capacity only as a percentage of the last full capacity
        int currentCapacity = (int) Math.round(remainingCapacityPercent * maxCapacity);
        int cycleCount = ParseUtil.parseIntOrDefault(psMap.getOrDefault("Cycle Count", ""), -1);

        return factory.create(name, psMap.getOrDefault("Model number", Constants.UNKNOWN), remainingCapacityPercent,
                timeRemainingEstimated, timeRemainingInstant, powerUsageRate, voltage, amperage, acline, charging,
                discharging, capacityUnits, currentCapacity, maxCapacity, designCapacity, cycleCount,
                psMap.getOrDefault("Type", Constants.UNKNOWN), null, psMap.getOrDefault("OEM info", Constants.UNKNOWN),
                psMap.getOrDefault("Serial number", Constants.UNKNOWN), 0d);
    }
}
