/*
 * Copyright 2016-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.unix.freebsd;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.platform.unix.freebsd.BsdSysctlUtil;

/**
 * A Power Source
 */
@ThreadSafe
public final class FreeBsdPowerSource extends AbstractPowerSource {

    public FreeBsdPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
     * @return A list of PowerSource objects representing batteries, etc.
     */
    public static List<PowerSource> getPowerSources() {
        return Arrays.asList(getPowerSource("BAT0"));
    }

    private static FreeBsdPowerSource getPowerSource(String name) {
        String psName = name;
        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
        double psPowerUsageRate = 0d;
        int psVoltage = -1;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        CapacityUnits psCapacityUnits = CapacityUnits.RELATIVE;
        int psCurrentCapacity = 0;
        int psMaxCapacity = 1;
        int psDesignCapacity = 1;
        int psCycleCount = -1;
        LocalDate psManufactureDate = null;

        double psTemperature = 0d;

        // state 0=full, 1=discharging, 2=charging
        int state = BsdSysctlUtil.sysctl("hw.acpi.battery.state", 0);
        if (state == 2) {
            psCharging = true;
        } else {
            int time = BsdSysctlUtil.sysctl("hw.acpi.battery.time", -1);
            // time is in minutes
            psTimeRemainingEstimated = time < 0 ? -1d : 60d * time;
            if (state == 1) {
                psDischarging = true;
            }
        }
        // life is in percent
        int life = BsdSysctlUtil.sysctl("hw.acpi.battery.life", -1);
        if (life > 0) {
            psRemainingCapacityPercent = life / 100d;
        }
        List<String> acpiconf = ExecutingCommand.runNative("acpiconf -i 0");
        Map<String, String> psMap = new HashMap<>();
        for (String line : acpiconf) {
            String[] split = line.split(":", 2);
            if (split.length > 1) {
                String value = split[1].trim();
                if (!value.isEmpty()) {
                    psMap.put(split[0], value);
                }
            }
        }

        String psDeviceName = psMap.getOrDefault("Model number", Constants.UNKNOWN);
        String psSerialNumber = psMap.getOrDefault("Serial number", Constants.UNKNOWN);
        String psChemistry = psMap.getOrDefault("Type", Constants.UNKNOWN);
        String psManufacturer = psMap.getOrDefault("OEM info", Constants.UNKNOWN);
        String cap = psMap.get("Design capacity");
        if (cap != null) {
            psDesignCapacity = ParseUtil.getFirstIntValue(cap);
            if (cap.toLowerCase().contains("mah")) {
                psCapacityUnits = CapacityUnits.MAH;
            } else if (cap.toLowerCase().contains("mwh")) {
                psCapacityUnits = CapacityUnits.MWH;
            }
        }
        cap = psMap.get("Last full capacity");
        if (cap != null) {
            psMaxCapacity = ParseUtil.getFirstIntValue(cap);
        } else {
            psMaxCapacity = psDesignCapacity;
        }
        double psTimeRemainingInstant = psTimeRemainingEstimated;
        String time = psMap.get("Remaining time");
        if (time != null) {
            String[] hhmm = time.split(":");
            if (hhmm.length == 2) {
                psTimeRemainingInstant = 3600d * ParseUtil.parseIntOrDefault(hhmm[0], 0)
                        + 60d * ParseUtil.parseIntOrDefault(hhmm[1], 0);
            }
        }
        String rate = psMap.get("Present rate");
        if (rate != null) {
            psPowerUsageRate = ParseUtil.getFirstIntValue(rate);
        }
        String volts = psMap.get("Present voltage");
        if (volts != null) {
            psVoltage = ParseUtil.getFirstIntValue(volts);
            if (psVoltage != 0) {
                psAmperage = psPowerUsageRate / psVoltage;
            }
        }

        return new FreeBsdPowerSource(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }
}
