/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import static oshi.software.os.linux.LinuxOperatingSystemJNA.HAS_UDEV;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.platform.linux.Udev;
import com.sun.jna.platform.linux.Udev.UdevContext;
import com.sun.jna.platform.linux.Udev.UdevDevice;
import com.sun.jna.platform.linux.Udev.UdevEnumerate;
import com.sun.jna.platform.linux.Udev.UdevListEntry;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.util.ParseUtil;

/**
 * JNA-based Linux power source implementation.
 */
@ThreadSafe
public final class LinuxPowerSourceJNA extends LinuxPowerSource {

    LinuxPowerSourceJNA(LinuxPowerSource src) {
        super(src);
    }

    @Override
    protected List<PowerSource> queryPowerSources() {
        return getPowerSources();
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
        UdevContext udev = Udev.INSTANCE.udev_new();
        if (udev == null) {
            return LinuxPowerSource.getPowerSources();
        }
        try {
            UdevEnumerate enumerate = udev.enumerateNew();
            try {
                enumerate.addMatchSubsystem("power_supply");
                enumerate.scanDevices();
                for (UdevListEntry entry = enumerate.getListEntry(); entry != null; entry = entry.getNext()) {
                    String syspath = entry.getName();
                    String name = syspath.substring(syspath.lastIndexOf(File.separatorChar) + 1);
                    if (name.startsWith("ADP") || name.startsWith("AC") || name.contains("USBC")) {
                        continue;
                    }
                    UdevDevice device = udev.deviceNewFromSyspath(syspath);
                    if (device != null) {
                        try {
                            if (ParseUtil.parseIntOrDefault(device.getPropertyValue(Prop.POWER_SUPPLY_PRESENT.name()),
                                    1) > 0) {
                                Map<Prop, String> props = new EnumMap<>(Prop.class);
                                for (Prop p : Prop.values()) {
                                    String val = device.getPropertyValue(p.name());
                                    if (val != null) {
                                        props.put(p, val);
                                    }
                                }
                                psList.add(new LinuxPowerSourceJNA(buildPowerSource(name, props)));
                            }
                        } finally {
                            device.unref();
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
}
