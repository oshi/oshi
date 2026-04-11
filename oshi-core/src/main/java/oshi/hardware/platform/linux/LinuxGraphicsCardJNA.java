/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.platform.linux.LinuxGraphicsCard;

/**
 * JNA-based Linux graphics card with NVML-backed GPU stats.
 */
@ThreadSafe
final class LinuxGraphicsCardJNA extends LinuxGraphicsCard {

    LinuxGraphicsCardJNA(String name, String deviceId, String vendor, String versionInfo, long vram,
            String drmDevicePath, String driverName, String pciBusId) {
        super(name, deviceId, vendor, versionInfo, vram, drmDevicePath, driverName, pciBusId);
    }

    @Override
    public GpuStats createStatsSession() {
        return new LinuxGpuStatsJNA(getDrmDevicePath(), getDriverName(), getPciBusId(), getName());
    }

    /**
     * Gets graphics cards using JNA-based GPU stats.
     *
     * @return list of graphics cards
     */
    public static List<GraphicsCard> getGraphicsCards() {
        return LinuxGraphicsCard
                .getGraphicsCards(a -> new LinuxGraphicsCardJNA(a.getName(), a.getDeviceId(), a.getVendor(),
                        a.getVersionInfo(), a.getVram(), a.getDrmDevicePath(), a.getDriverName(), a.getPciBusId()));
    }
}
