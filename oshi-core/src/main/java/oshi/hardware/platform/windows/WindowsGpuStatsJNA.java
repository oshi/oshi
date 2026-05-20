/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.driver.windows.perfmon.GpuInformationJNA;
import oshi.driver.windows.wmi.LhmSensorJNA;
import oshi.hardware.common.platform.windows.WindowsGpuStats;
import oshi.util.gpu.AdlUtilJNA;
import oshi.util.gpu.NvmlUtilJNA;
import oshi.util.tuples.Pair;

/**
 * Windows {@link oshi.hardware.GpuStats} session using JNA.
 */
@ThreadSafe
final class WindowsGpuStatsJNA extends WindowsGpuStats {

    WindowsGpuStatsJNA(String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId, String cardName) {
        super(luidPrefix, lhmParent, pciBusNumber, pciBusId, cardName);
    }

    @Override
    protected Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters() {
        return GpuInformationJNA.queryGpuEngineCounters();
    }

    @Override
    protected Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters() {
        return GpuInformationJNA.queryGpuAdapterMemoryCounters();
    }

    @Override
    protected WmiResult<LhmSensorProperty> queryLhmSensors(String parent, String sensorType) {
        return LhmSensorJNA.querySensors(parent, sensorType);
    }

    @Override
    protected boolean isNvmlAvailable() {
        return NvmlUtilJNA.isAvailable();
    }

    @Override
    protected String nvmlFindDevice(String pciBusId) {
        return NvmlUtilJNA.findDevice(pciBusId);
    }

    @Override
    protected String nvmlFindDeviceByName(String gpuName) {
        return NvmlUtilJNA.findDeviceByName(gpuName);
    }

    @Override
    protected double nvmlGetTemperature(String device) {
        return NvmlUtilJNA.getTemperature(device);
    }

    @Override
    protected double nvmlGetPowerDraw(String device) {
        return NvmlUtilJNA.getPowerDraw(device);
    }

    @Override
    protected long nvmlGetCoreClockMhz(String device) {
        return NvmlUtilJNA.getCoreClockMhz(device);
    }

    @Override
    protected long nvmlGetMemoryClockMhz(String device) {
        return NvmlUtilJNA.getMemoryClockMhz(device);
    }

    @Override
    protected double nvmlGetFanSpeedPercent(String device) {
        return NvmlUtilJNA.getFanSpeedPercent(device);
    }

    @Override
    protected boolean isAdlAvailable() {
        return AdlUtilJNA.isAvailable();
    }

    @Override
    protected int adlFindAdapterIndex(int pciBusNumber) {
        return AdlUtilJNA.findAdapterIndex(pciBusNumber);
    }

    @Override
    protected double adlGetTemperature(int adapterIndex) {
        return AdlUtilJNA.getTemperature(adapterIndex);
    }

    @Override
    protected double adlGetPowerDraw(int adapterIndex) {
        return AdlUtilJNA.getPowerDraw(adapterIndex);
    }

    @Override
    protected long adlGetCoreClockMhz(int adapterIndex) {
        return AdlUtilJNA.getCoreClockMhz(adapterIndex);
    }

    @Override
    protected long adlGetMemoryClockMhz(int adapterIndex) {
        return AdlUtilJNA.getMemoryClockMhz(adapterIndex);
    }

    @Override
    protected double adlGetFanSpeedPercent(int adapterIndex) {
        return AdlUtilJNA.getFanSpeedPercent(adapterIndex);
    }
}
