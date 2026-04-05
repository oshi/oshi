/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemFFM.HAS_UDEV;

import java.io.File;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.linux.UdevFunctions;
import oshi.hardware.PowerSource;
import oshi.util.ParseUtil;

/**
 * FFM-based Linux power source implementation.
 */
@ThreadSafe
public final class LinuxPowerSourceFFM extends LinuxPowerSource {

    private static final Logger LOG = LoggerFactory.getLogger(LinuxPowerSourceFFM.class);

    public LinuxPowerSourceFFM(String psName, String psDeviceName, double psRemainingCapacityPercent,
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
        if (!HAS_UDEV) {
            return LinuxPowerSource.getPowerSources();
        }
        List<PowerSource> psList = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment udev = UdevFunctions.udev_new();
            try {
                MemorySegment enumerate = UdevFunctions.udev_enumerate_new(udev);
                try {
                    UdevFunctions.addMatchSubsystem(enumerate, "power_supply", arena);
                    UdevFunctions.udev_enumerate_scan_devices(enumerate);
                    for (MemorySegment entry = UdevFunctions
                            .udev_enumerate_get_list_entry(enumerate); !MemorySegment.NULL
                                    .equals(entry); entry = UdevFunctions.udev_list_entry_get_next(entry)) {
                        String syspath = UdevFunctions.getString(UdevFunctions.udev_list_entry_get_name(entry), arena);
                        if (syspath == null) {
                            continue;
                        }
                        String name = syspath.substring(syspath.lastIndexOf(File.separatorChar) + 1);
                        if (name.startsWith("ADP") || name.startsWith("AC") || name.contains("USBC")) {
                            continue;
                        }
                        MemorySegment device = UdevFunctions.deviceNewFromSyspath(udev, syspath, arena);
                        if (MemorySegment.NULL.equals(device)) {
                            continue;
                        }
                        try {
                            if (ParseUtil.parseIntOrDefault(
                                    UdevFunctions.getPropertyValue(device, Prop.POWER_SUPPLY_PRESENT.name(), arena),
                                    1) > 0) {
                                Map<Prop, String> props = new EnumMap<>(Prop.class);
                                for (Prop p : Prop.values()) {
                                    String val = UdevFunctions.getPropertyValue(device, p.name(), arena);
                                    if (val != null) {
                                        props.put(p, val);
                                    }
                                }
                                psList.add(buildPowerSource(name, props));
                            }
                        } finally {
                            UdevFunctions.udev_device_unref(device);
                        }
                    }
                } finally {
                    UdevFunctions.udev_enumerate_unref(enumerate);
                }
            } finally {
                UdevFunctions.udev_unref(udev);
            }
        } catch (Throwable e) {
            LOG.warn("Error enumerating power sources via udev, falling back to sysfs: {}", e.toString());
            return LinuxPowerSource.getPowerSources();
        }
        return psList;
    }
}
