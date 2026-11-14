/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.netbsd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.unix.netbsd.Envstat;
import oshi.driver.common.unix.netbsd.Envstat.Sensor;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;

/**
 * Batteries reported by NetBSD's {@code acpibat(4)} through {@code envstat}, with the external power state from
 * {@code acpiacad(4)}.
 */
@ThreadSafe
final class NetBsdPowerSource extends AbstractPowerSource {

    private static final double MICRO = 1_000_000d;

    private NetBsdPowerSource(String psName, double psRemainingCapacityPercent, double psTimeRemaining,
            double psPowerUsageRate, double psVoltage, double psAmperage, boolean psPowerOnLine, boolean psCharging,
            boolean psDischarging, CapacityUnits psCapacityUnits, int psCurrentCapacity, int psMaxCapacity,
            int psDesignCapacity) {
        super(psName, Constants.UNKNOWN, psRemainingCapacityPercent, psTimeRemaining, psTimeRemaining, psPowerUsageRate,
                psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging, psCapacityUnits, psCurrentCapacity,
                psMaxCapacity, psDesignCapacity, -1, Constants.UNKNOWN, null, Constants.UNKNOWN, Constants.UNKNOWN, 0d);
    }

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
    }

    /**
     * Gets the batteries present in the system.
     *
     * @return the power sources
     */
    static List<PowerSource> getPowerSources() {
        return parsePowerSources(Envstat.querySensors());
    }

    /**
     * Builds power sources from {@code envstat} sensors. A battery is a device reporting {@code present} and
     * {@code charge}; a slot whose {@code present} indicator is false or invalid is skipped.
     *
     * @param sensors the parsed sensors
     * @return a power source for each battery present
     */
    static List<PowerSource> parsePowerSources(List<Sensor> sensors) {
        Map<String, Map<String, Sensor>> byDevice = new LinkedHashMap<>();
        @Nullable
        Boolean acConnected = null;
        for (Sensor s : sensors) {
            byDevice.computeIfAbsent(s.getDevice(), d -> new LinkedHashMap<>()).put(s.getDescription(), s);
            if ("connected".equals(s.getDescription()) && Envstat.INDICATOR.equals(s.getType()) && s.isValid()) {
                acConnected = (acConnected != null && acConnected) || s.getValue() != 0;
            }
        }
        List<PowerSource> batteries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Sensor>> e : byDevice.entrySet()) {
            Map<String, Sensor> bat = e.getValue();
            Sensor present = bat.get("present");
            Sensor charge = bat.get("charge");
            // A failed presence check leaves the previous value in place but marks every sensor invalid
            if (present == null || charge == null || !present.isValid() || present.getValue() == 0) {
                continue;
            }
            batteries.add(toPowerSource(e.getKey(), bat, charge, acConnected));
        }
        return Collections.unmodifiableList(batteries);
    }

    private static PowerSource toPowerSource(String device, Map<String, Sensor> bat, Sensor charge,
            @Nullable Boolean acConnected) {
        CapacityUnits units = Envstat.AMPERE_HOUR.equals(charge.getType()) ? CapacityUnits.MAH : CapacityUnits.MWH;
        // Energy and charge are in micro units; OSHI reports milli units
        int current = (int) (value(charge) / 1000);
        int max = (int) (value(bat.get("last full cap")) / 1000);
        int design = (int) (value(bat.get("design cap")) / 1000);
        double percent = max > 0 ? Math.min(1d, (double) current / max) : 1d;

        Sensor voltageSensor = bat.get("voltage");
        double voltage = voltageSensor != null && voltageSensor.isValid() ? voltageSensor.getValue() / MICRO : -1d;
        long chargeRate = value(bat.get("charge rate"));
        long dischargeRate = value(bat.get("discharge rate"));
        boolean charging = value(bat.get("charging")) != 0;
        boolean discharging = !charging && dischargeRate > 0;
        boolean onLine = acConnected != null ? acConnected : !discharging;

        // The rate sensors are watts for a battery measured in watt-hours and amperes for one in ampere-hours
        long signedRate = charging ? chargeRate : -dischargeRate;
        double powerMilliwatts = 0d;
        double currentMilliamps = 0d;
        if (units == CapacityUnits.MWH) {
            powerMilliwatts = signedRate / 1000d;
            if (voltage > 0) {
                currentMilliamps = powerMilliwatts / voltage;
            }
        } else {
            currentMilliamps = signedRate / 1000d;
            if (voltage > 0) {
                powerMilliwatts = currentMilliamps * voltage;
            }
        }

        // -1 is unknown, -2 is unlimited (running on external power)
        double timeRemaining = -1d;
        if (discharging) {
            timeRemaining = 3600d * value(charge) / dischargeRate;
        } else if (onLine) {
            timeRemaining = -2d;
        }

        String name = device.startsWith("acpi") ? device.substring(4) : device;
        return new NetBsdPowerSource(name, percent, timeRemaining, powerMilliwatts, voltage, currentMilliamps, onLine,
                charging, discharging, units, current, max, design);
    }

    private static long value(@Nullable Sensor s) {
        return s != null && s.isValid() ? s.getValue() : 0L;
    }
}
