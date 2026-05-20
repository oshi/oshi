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
import oshi.driver.windows.perfmon.GpuInformationFFM;
import oshi.driver.windows.wmi.LhmSensorFFM;
import oshi.ffm.util.gpu.AdlUtilFFM;
import oshi.ffm.util.gpu.NvmlUtilFFM;
import oshi.hardware.common.platform.windows.WindowsGpuStats;
import oshi.util.tuples.Pair;

/**
 * Windows {@link oshi.hardware.GpuStats} session using FFM.
 */
@ThreadSafe
final class WindowsGpuStatsFFM extends WindowsGpuStats {

    WindowsGpuStatsFFM(String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId, String cardName) {
        super(luidPrefix, lhmParent, pciBusNumber, pciBusId, cardName);
    }

    @Override
    protected Pair<List<String>, Map<GpuEngineProperty, List<Long>>> queryGpuEngineCounters() {
        return GpuInformationFFM.queryGpuEngineCounters();
    }

    @Override
    protected Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> queryGpuAdapterMemoryCounters() {
        return GpuInformationFFM.queryGpuAdapterMemoryCounters();
    }

    @Override
    protected WmiResult<LhmSensorProperty> queryLhmSensors(String parent, String sensorType) {
        return LhmSensorFFM.querySensors(parent, sensorType);
    }

    @Override
    protected boolean isNvmlAvailable() {
        return NvmlUtilFFM.isAvailable();
    }

    @Override
    protected String nvmlFindDevice(String pciBusId) {
        return NvmlUtilFFM.findDevice(pciBusId);
    }

    @Override
    protected String nvmlFindDeviceByName(String gpuName) {
        return NvmlUtilFFM.findDeviceByName(gpuName);
    }

    @Override
    protected double nvmlGetTemperature(String device) {
        return NvmlUtilFFM.getTemperature(device);
    }

    @Override
    protected double nvmlGetPowerDraw(String device) {
        return NvmlUtilFFM.getPowerDraw(device);
    }

    @Override
    protected long nvmlGetCoreClockMhz(String device) {
        return NvmlUtilFFM.getCoreClockMhz(device);
    }

    @Override
    protected long nvmlGetMemoryClockMhz(String device) {
        return NvmlUtilFFM.getMemoryClockMhz(device);
    }

    @Override
    protected double nvmlGetFanSpeedPercent(String device) {
        return NvmlUtilFFM.getFanSpeedPercent(device);
    }

    @Override
    protected boolean isAdlAvailable() {
        return AdlUtilFFM.isAvailable();
    }

    @Override
    protected int adlFindAdapterIndex(int pciBusNumber) {
        return AdlUtilFFM.findAdapterIndex(pciBusNumber);
    }

    @Override
    protected double adlGetTemperature(int adapterIndex) {
        return AdlUtilFFM.getTemperature(adapterIndex);
    }

    @Override
    protected double adlGetPowerDraw(int adapterIndex) {
        return AdlUtilFFM.getPowerDraw(adapterIndex);
    }

    @Override
    protected long adlGetCoreClockMhz(int adapterIndex) {
        return AdlUtilFFM.getCoreClockMhz(adapterIndex);
    }

    @Override
    protected long adlGetMemoryClockMhz(int adapterIndex) {
        return AdlUtilFFM.getMemoryClockMhz(adapterIndex);
    }

    @Override
    protected double adlGetFanSpeedPercent(int adapterIndex) {
        return AdlUtilFFM.getFanSpeedPercent(adapterIndex);
    }
}
