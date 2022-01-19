/*
 * MIT License
 *
 * Copyright (c) 2020-2022 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.sun.jna.platform.linux.Udev; // NOSONAR squid:s1191
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.common.AbstractPowerSource;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * A Power Source
 */
@ThreadSafe
public final class LinuxPowerSource extends AbstractPowerSource {

    public LinuxPowerSource(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
    public static List<PowerSource> getPowerSources() {
        String psName;
        String psDeviceName;
        double psRemainingCapacityPercent = -1d;
        double psTimeRemainingEstimated = -1d; // -1 = unknown, -2 = unlimited
        double psTimeRemainingInstant = -1d;
        double psPowerUsageRate = 0d;
        double psVoltage = -1d;
        double psAmperage = 0d;
        boolean psPowerOnLine = false;
        boolean psCharging = false;
        boolean psDischarging = false;
        CapacityUnits psCapacityUnits = CapacityUnits.RELATIVE;
        int psCurrentCapacity = -1;
        int psMaxCapacity = -1;
        int psDesignCapacity = -1;
        int psCycleCount = -1;
        String psChemistry;
        LocalDate psManufactureDate = null;
        String psManufacturer;
        String psSerialNumber;
        double psTemperature = 0d;

        List<PowerSource> psList = new ArrayList<>();
        UdevContext udev = Udev.INSTANCE.udev_new();
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem("power_supply");
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    String name = syspath.substring(syspath.lastIndexOf(File.separatorChar) + 1);
                    if (!name.startsWith("ADP") && !name.startsWith("AC")) {
                        UdevDevice device = udev.deviceNewFromSyspath(syspath);
                        if (device != null) {
                            try {
                                if (ParseUtil.parseIntOrDefault(device.getPropertyValue("POWER_SUPPLY_PRESENT"),
                                        1) > 0) {
                                    psName = getOrDefault(device, "POWER_SUPPLY_NAME", name);
                                    String status = device.getPropertyValue("POWER_SUPPLY_STATUS");
                                    psCharging = "Charging".equals(status);
                                    psDischarging = "Discharging".equals(status);
                                    psRemainingCapacityPercent = ParseUtil.parseIntOrDefault(
                                            device.getPropertyValue("POWER_SUPPLY_CAPACITY"), -100) / 100d;

                                    // Debian/Ubuntu provides Energy. Fedora/RHEL provides Charge.
                                    psCurrentCapacity = ParseUtil
                                            .parseIntOrDefault(device.getPropertyValue("POWER_SUPPLY_ENERGY_NOW"), -1);
                                    if (psCurrentCapacity < 0) {
                                        psCurrentCapacity = ParseUtil.parseIntOrDefault(
                                                device.getPropertyValue("POWER_SUPPLY_CHARGE_NOW"), -1);
                                    }
                                    psMaxCapacity = ParseUtil
                                            .parseIntOrDefault(device.getPropertyValue("POWER_SUPPLY_ENERGY_FULL"), 1);
                                    if (psMaxCapacity < 0) {
                                        psMaxCapacity = ParseUtil.parseIntOrDefault(
                                                device.getPropertyValue("POWER_SUPPLY_CHARGE_FULL"), 1);
                                    }
                                    psDesignCapacity = ParseUtil.parseIntOrDefault(
                                            device.getPropertyValue("POWER_SUPPLY_ENERGY_FULL_DESIGN"), 1);
                                    if (psDesignCapacity < 0) {
                                        psDesignCapacity = ParseUtil.parseIntOrDefault(
                                                device.getPropertyValue("POWER_SUPPLY_CHARGE_FULL_DESIGN"), 1);
                                    }

                                    // Debian/Ubuntu provides Voltage and Power.
                                    // Fedora/RHEL provides Voltage and Current.
                                    psVoltage = ParseUtil
                                            .parseIntOrDefault(device.getPropertyValue("POWER_SUPPLY_VOLTAGE_NOW"), -1);
                                    // From Physics we know P = IV so I = P/V
                                    if (psVoltage > 0) {
                                        String power = device.getPropertyValue("POWER_SUPPLY_POWER_NOW");
                                        String current = device.getPropertyValue("POWER_SUPPLY_CURRENT_NOW");
                                        if (power == null) {
                                            psAmperage = ParseUtil.parseIntOrDefault(current, 0);
                                            psPowerUsageRate = psAmperage * psVoltage;
                                        } else if (current == null) {
                                            psPowerUsageRate = ParseUtil.parseIntOrDefault(power, 0);
                                            psAmperage = psPowerUsageRate / psVoltage;
                                        } else {
                                            psAmperage = ParseUtil.parseIntOrDefault(current, 0);
                                            psPowerUsageRate = ParseUtil.parseIntOrDefault(power, 0);
                                        }
                                    }

                                    psCycleCount = ParseUtil
                                            .parseIntOrDefault(device.getPropertyValue("POWER_SUPPLY_CYCLE_COUNT"), -1);
                                    psChemistry = getOrDefault(device, "POWER_SUPPLY_TECHNOLOGY", Constants.UNKNOWN);
                                    psDeviceName = getOrDefault(device, "POWER_SUPPLY_MODEL_NAME", Constants.UNKNOWN);
                                    psManufacturer = getOrDefault(device, "POWER_SUPPLY_MANUFACTURER",
                                            Constants.UNKNOWN);
                                    psSerialNumber = getOrDefault(device, "POWER_SUPPLY_SERIAL_NUMBER",
                                            Constants.UNKNOWN);

                                    psList.add(new LinuxPowerSource(psName, psDeviceName, psRemainingCapacityPercent,
                                            psTimeRemainingEstimated, psTimeRemainingInstant, psPowerUsageRate,
                                            psVoltage, psAmperage, psPowerOnLine, psCharging, psDischarging,
                                            psCapacityUnits, psCurrentCapacity, psMaxCapacity, psDesignCapacity,
                                            psCycleCount, psChemistry, psManufactureDate, psManufacturer,
                                            psSerialNumber, psTemperature));
                                }
                            } finally {
                                device.unref();
                            }
                        }
                    }
                }
            } finally {
                enumerate.unref();
            }
        } finally {
            udev.unref();
        }
        return psList;
    }

    private static String getOrDefault(UdevDevice device, String property, String def) {
        String value = device.getPropertyValue(property);
        return value == null ? def : value;
    }
}
