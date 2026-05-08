/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.linux.nativefree;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.platform.linux.LinuxGpuStats;
import oshi.hardware.common.platform.linux.LinuxGraphicsCard;

/**
 * Native-free Linux graphics card implementation. Uses {@code lspci}/{@code lshw} for enumeration (already in
 * superclass) and provides a {@link GpuStats} that reads sysfs for AMD/Intel but has no NVML support.
 */
@ThreadSafe
final class LinuxGraphicsCardNF extends LinuxGraphicsCard {

    LinuxGraphicsCardNF(String name, String deviceId, String vendor, String versionInfo, long vram,
            String drmDevicePath, String driverName, String pciBusId) {
        super(name, deviceId, vendor, versionInfo, vram, drmDevicePath, driverName, pciBusId);
    }

    @Override
    public GpuStats createStatsSession() {
        return new LinuxGpuStatsNF(getDrmDevicePath(), getDriverName(), getPciBusId(), getName());
    }

    /**
     * Gets graphics cards using command-line tools (lspci/lshw).
     *
     * @return list of graphics cards
     */
    public static List<GraphicsCard> getGraphicsCards() {
        return LinuxGraphicsCard
                .getGraphicsCards(a -> new LinuxGraphicsCardNF(a.getName(), a.getDeviceId(), a.getVendor(),
                        a.getVersionInfo(), a.getVram(), a.getDrmDevicePath(), a.getDriverName(), a.getPciBusId()));
    }

    /**
     * Native-free GPU stats — sysfs-based metrics for AMD/Intel, no NVML.
     */
    private static final class LinuxGpuStatsNF extends LinuxGpuStats {

        LinuxGpuStatsNF(String drmDevicePath, String driverName, String pciBusId, String name) {
            super(drmDevicePath, driverName, pciBusId, name);
        }

        @Override
        protected boolean nvmlIsAvailable() {
            return false;
        }

        @Override
        protected String nvmlFindDevice(String busId) {
            return null;
        }

        @Override
        protected String nvmlFindDeviceByName(String name) {
            return null;
        }

        @Override
        protected long nvmlGetVramUsed(String deviceId) {
            return -1L;
        }

        @Override
        protected double nvmlGetTemperature(String deviceId) {
            return -1d;
        }

        @Override
        protected double nvmlGetPowerDraw(String deviceId) {
            return -1d;
        }

        @Override
        protected long nvmlGetCoreClockMhz(String deviceId) {
            return -1L;
        }

        @Override
        protected long nvmlGetMemoryClockMhz(String deviceId) {
            return -1L;
        }

        @Override
        protected double nvmlGetFanSpeedPercent(String deviceId) {
            return -1d;
        }
    }
}
