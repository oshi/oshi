/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.util.gpu.NvmlUtilFFM;
import oshi.hardware.common.platform.linux.LinuxGpuStats;

/**
 * FFM-based Linux {@link LinuxGpuStats} subclass providing NVML integration via FFM.
 */
@ThreadSafe
final class LinuxGpuStatsFFM extends LinuxGpuStats {

    LinuxGpuStatsFFM(String drmDevicePath, String driverName, String pciBusId, String cardName) {
        super(drmDevicePath, driverName, pciBusId, cardName);
    }

    @Override
    protected boolean nvmlIsAvailable() {
        return NvmlUtilFFM.isAvailable();
    }

    @Override
    protected String nvmlFindDevice(String busId) {
        return NvmlUtilFFM.findDevice(busId);
    }

    @Override
    protected String nvmlFindDeviceByName(String name) {
        return NvmlUtilFFM.findDeviceByName(name);
    }

    @Override
    protected long nvmlGetVramUsed(String deviceId) {
        return NvmlUtilFFM.getVramUsed(deviceId);
    }

    @Override
    protected double nvmlGetTemperature(String deviceId) {
        return NvmlUtilFFM.getTemperature(deviceId);
    }

    @Override
    protected double nvmlGetPowerDraw(String deviceId) {
        return NvmlUtilFFM.getPowerDraw(deviceId);
    }

    @Override
    protected long nvmlGetCoreClockMhz(String deviceId) {
        return NvmlUtilFFM.getCoreClockMhz(deviceId);
    }

    @Override
    protected long nvmlGetMemoryClockMhz(String deviceId) {
        return NvmlUtilFFM.getMemoryClockMhz(deviceId);
    }

    @Override
    protected double nvmlGetFanSpeedPercent(String deviceId) {
        return NvmlUtilFFM.getFanSpeedPercent(deviceId);
    }
}
