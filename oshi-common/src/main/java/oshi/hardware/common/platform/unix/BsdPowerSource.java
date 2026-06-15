/*
 * Copyright 2021-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.bsd.Systat;
import oshi.driver.common.unix.bsd.Systat.BatteryFields;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Shared power-source implementation for BSDs that use {@code apm} and {@code systat -ab sensors} for battery state
 * (NetBSD, OpenBSD).
 */
@ThreadSafe
public final class BsdPowerSource extends AbstractPowerSource {

    private BsdPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
        List<String> sensorLines = Systat.querySensorLines();
        List<PowerSource> psList = new ArrayList<>();
        for (String name : Systat.parsePowerSourceNames(sensorLines)) {
            psList.add(getPowerSource(name, sensorLines));
        }
        return psList;
    }

    private static BsdPowerSource getPowerSource(String name, List<String> sensorLines) {
        String psName = name.startsWith("acpi") ? name.substring(4) : name;
        BatteryFields b = Systat.parseBatteryFields(name, sensorLines);
        double psVoltage = b.getVoltage();
        double psAmperage = b.getAmperage();
        double psTemperature = b.getTemperature();
        int psCurrentCapacity = b.getCurrentCapacity();
        int psMaxCapacity = b.getMaxCapacity();
        int psDesignCapacity = b.getDesignCapacity();
        CapacityUnits psCapacityUnits = b.getCapacityUnits();
        double psPowerUsageRate = 0d;

        double psRemainingCapacityPercent = 1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        int psCycleCount = -1;
        LocalDate psManufactureDate = null;

        int state = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("apm -b"), 255);
        // state 0=high, 1=low, 2=critical, 3=charging, 4=absent, 255=unknown
        if (state < 4) {
            psPowerOnLine = true;
            if (state == 3) {
                psCharging = true;
            } else {
                int time = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("apm -m"), -1);
                // time is in minutes
                psTimeRemainingEstimated = time < 0 ? -1d : 60d * time;
                psDischarging = true;
            }
        }
        // life is in percent
        int life = ParseUtil.parseIntOrDefault(ExecutingCommand.getFirstAnswer("apm -l"), -1);
        if (life > 0) {
            psRemainingCapacityPercent = life / 100d;
        }
        if (psMaxCapacity < psDesignCapacity && psMaxCapacity < psCurrentCapacity) {
            psMaxCapacity = psDesignCapacity;
        } else if (psDesignCapacity < psMaxCapacity && psDesignCapacity < psCurrentCapacity) {
            psDesignCapacity = psMaxCapacity;
        }

        String psDeviceName = Constants.UNKNOWN;
        String psSerialNumber = Constants.UNKNOWN;
        String psChemistry = Constants.UNKNOWN;
        String psManufacturer = Constants.UNKNOWN;

        double psTimeRemainingInstant = psTimeRemainingEstimated;
        // apm reports amperage but not power; derive power rate from P = IV
        if (psVoltage > 0 && psAmperage > 0) {
            psPowerUsageRate = psAmperage * psVoltage;
        }

        return new BsdPowerSource(psName, psDeviceName, psRemainingCapacityPercent, psTimeRemainingEstimated,
                psTimeRemainingInstant, psPowerUsageRate, psVoltage, psAmperage, psPowerOnLine, psCharging,
                psDischarging, psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity, psCycleCount,
                psChemistry, psManufactureDate, psManufacturer, psSerialNumber, psTemperature);
    }
}
