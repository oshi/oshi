/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.common.platform.linux.LinuxGpuStats;
import oshi.util.gpu.NvmlUtilJNA;

/**
 * JNA-based Linux {@link LinuxGpuStats} subclass providing NVML integration via JNA.
 */
@ThreadSafe
final class LinuxGpuStatsJNA extends LinuxGpuStats {

    LinuxGpuStatsJNA(String drmDevicePath, String driverName, String pciBusId, String cardName) {
        super(drmDevicePath, driverName, pciBusId, cardName);
    }

    @Override
    protected boolean nvmlIsAvailable() {
        return NvmlUtilJNA.isAvailable();
    }

    @Override
    protected String nvmlFindDevice(String busId) {
        return NvmlUtilJNA.findDevice(busId);
    }

    @Override
    protected String nvmlFindDeviceByName(String name) {
        return NvmlUtilJNA.findDeviceByName(name);
    }

    @Override
    protected long nvmlGetVramUsed(String deviceId) {
        return NvmlUtilJNA.getVramUsed(deviceId);
    }

    @Override
    protected double nvmlGetTemperature(String deviceId) {
        return NvmlUtilJNA.getTemperature(deviceId);
    }

    @Override
    protected double nvmlGetPowerDraw(String deviceId) {
        return NvmlUtilJNA.getPowerDraw(deviceId);
    }

    @Override
    protected long nvmlGetCoreClockMhz(String deviceId) {
        return NvmlUtilJNA.getCoreClockMhz(deviceId);
    }

    @Override
    protected long nvmlGetMemoryClockMhz(String deviceId) {
        return NvmlUtilJNA.getMemoryClockMhz(deviceId);
    }

    @Override
    protected double nvmlGetFanSpeedPercent(String deviceId) {
        return NvmlUtilJNA.getFanSpeedPercent(deviceId);
    }
}
