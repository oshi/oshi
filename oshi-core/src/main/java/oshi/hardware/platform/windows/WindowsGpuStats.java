/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.windows.perfmon.GpuInformation;
import oshi.driver.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.driver.windows.wmi.LhmSensor;
import oshi.driver.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;
import oshi.util.gpu.AdlUtil;
import oshi.util.gpu.NvmlUtil;
import oshi.util.platform.windows.WmiUtil;
import oshi.util.tuples.Pair;

/**
 * Windows {@link GpuStats} session.
 *
 * <p>
 * Metric source priority by method:
 * <ul>
 * <li>{@code getGpuTicks()}: PDH GPU Engine counters ({@code Running Time} / {@code Running Time_Base}).</li>
 * <li>{@code getGpuUtilization()}: LHM WMI {@code GPU Core} load sensor. Returns -1 when LHM is not running; use
 * {@code getGpuTicks()} as an alternative.</li>
 * <li>{@code getVramUsed()}: PDH GPU Adapter Memory {@code DedicatedUsage}, then LHM {@code GPU Memory Used}.</li>
 * <li>{@code getSharedMemoryUsed()}: PDH GPU Adapter Memory {@code SharedUsage}.</li>
 * <li>{@code getTemperature()}: NVML, then ADL, then LHM {@code GPU Core} temperature.</li>
 * <li>{@code getPowerDraw()}: NVML, then ADL, then LHM {@code GPU Package} / {@code GPU Power}.</li>
 * <li>{@code getCoreClockMhz()}: NVML, then ADL, then LHM {@code GPU Core} clock.</li>
 * <li>{@code getMemoryClockMhz()}: NVML, then ADL, then LHM {@code GPU Memory} clock.</li>
 * <li>{@code getFanSpeedPercent()}: NVML, then ADL, then LHM {@code GPU Fan} / {@code GPU Fan 1}.</li>
 * </ul>
 *
 * <p>
 * PDH metrics require a valid LUID prefix (populated from DXGI). NVML requires an NVIDIA GPU with the NVML library
 * present. ADL requires an AMD GPU with the ADL library present. LHM requires LibreHardwareMonitor to be running.
 */
@ThreadSafe
final class WindowsGpuStats implements GpuStats {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGpuStats.class);

    private static final long MB_TO_BYTES = 1_048_576L;

    private final String luidPrefix;
    private final String lhmParent;
    private final int pciBusNumber;
    private final String pciBusId;
    private final String cardName;

    private boolean closed;

    // Cached device lookups; null = not yet resolved, empty = unavailable
    private String cachedNvmlDevice;
    // Integer.MIN_VALUE = not yet resolved, -1 = unavailable
    private int cachedAdlIndex = Integer.MIN_VALUE;

    WindowsGpuStats(String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId, String cardName) {
        this.luidPrefix = luidPrefix;
        this.lhmParent = lhmParent;
        this.pciBusNumber = pciBusNumber;
        this.pciBusId = pciBusId;
        this.cardName = cardName;
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized GpuTicks getGpuTicks() {
        checkOpen();
        if (luidPrefix.isEmpty()) {
            return new GpuTicks(0L, 0L);
        }
        Pair<List<String>, Map<GpuEngineProperty, List<Long>>> engineData = GpuInformation.queryGpuEngineCounters();
        List<String> instances = engineData.getA();
        Map<GpuEngineProperty, List<Long>> values = engineData.getB();
        List<Long> runningTimes = values.get(GpuEngineProperty.RUNNING_TIME);
        List<Long> runningTimeBases = values.get(GpuEngineProperty.RUNNING_TIME_BASE);
        if (instances.isEmpty() || runningTimes == null || runningTimeBases == null) {
            return new GpuTicks(0L, 0L);
        }
        Map<String, Long> activeByType = new HashMap<>();
        Map<String, Long> baseByType = new HashMap<>();
        String luidLower = luidPrefix.toLowerCase(Locale.ROOT);
        int limit = Math.min(instances.size(), Math.min(runningTimes.size(), runningTimeBases.size()));
        for (int i = 0; i < limit; i++) {
            String inst = instances.get(i).toLowerCase(Locale.ROOT);
            if (!inst.contains(luidLower)) {
                continue;
            }
            int engTypeIdx = inst.lastIndexOf("_engtype_");
            String engType = engTypeIdx >= 0 ? inst.substring(engTypeIdx) : inst;
            activeByType.merge(engType, runningTimes.get(i), Long::sum);
            baseByType.merge(engType, runningTimeBases.get(i), Long::sum);
        }
        if (activeByType.isEmpty()) {
            return new GpuTicks(0L, 0L);
        }
        long totalActive = 0L;
        long totalBase = 0L;
        for (String key : activeByType.keySet()) {
            totalActive += activeByType.get(key);
            totalBase += baseByType.getOrDefault(key, 0L);
        }
        long idle = totalBase >= totalActive ? totalBase - totalActive : 0L;
        return new GpuTicks(totalActive, idle);
    }

    @Override
    public synchronized double getGpuUtilization() {
        checkOpen();
        if (!lhmParent.isEmpty()) {
            try {
                WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(lhmParent, "Load");
                for (int i = 0; i < sensors.getResultCount(); i++) {
                    if ("GPU Core".equals(WmiUtil.getString(sensors, LhmSensorProperty.NAME, i))) {
                        return WmiUtil.getFloat(sensors, LhmSensorProperty.VALUE, i);
                    }
                }
            } catch (Exception e) {
                LOG.debug("LHM GPU utilization query failed: {}", e.getMessage());
            }
        }
        return -1d;
    }

    @Override
    public synchronized long getVramUsed() {
        checkOpen();
        long pdhResult = queryAdapterMemory(GpuAdapterMemoryProperty.DEDICATED_USAGE);
        if (pdhResult >= 0) {
            return pdhResult;
        }
        if (!lhmParent.isEmpty()) {
            try {
                WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(lhmParent, "SmallData");
                for (int i = 0; i < sensors.getResultCount(); i++) {
                    if ("GPU Memory Used".equals(WmiUtil.getString(sensors, LhmSensorProperty.NAME, i))) {
                        float mb = WmiUtil.getFloat(sensors, LhmSensorProperty.VALUE, i);
                        return (long) (mb * MB_TO_BYTES);
                    }
                }
            } catch (Exception e) {
                LOG.debug("LHM GPU memory used query failed: {}", e.getMessage());
            }
        }
        return -1L;
    }

    @Override
    public synchronized long getSharedMemoryUsed() {
        checkOpen();
        if (luidPrefix.isEmpty()) {
            return -1L;
        }
        return queryAdapterMemory(GpuAdapterMemoryProperty.SHARED_USAGE);
    }

    @Override
    public synchronized double getTemperature() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getTemperature(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtil.getTemperature(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        return lhmFloatSensor("Temperature", "GPU Core");
    }

    @Override
    public synchronized double getPowerDraw() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getPowerDraw(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtil.getPowerDraw(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        double lhm = lhmFloatSensor("Power", "GPU Package");
        if (lhm >= 0) {
            return lhm;
        }
        return lhmFloatSensor("Power", "GPU Power");
    }

    @Override
    public synchronized long getCoreClockMhz() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getCoreClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            long val = AdlUtil.getCoreClockMhz(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        double lhm = lhmFloatSensor("Clock", "GPU Core");
        return lhm >= 0 ? (long) lhm : -1L;
    }

    @Override
    public synchronized long getMemoryClockMhz() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getMemoryClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            long val = AdlUtil.getMemoryClockMhz(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        double lhm = lhmFloatSensor("Clock", "GPU Memory");
        return lhm >= 0 ? (long) lhm : -1L;
    }

    @Override
    public synchronized double getFanSpeedPercent() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getFanSpeedPercent(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtil.getFanSpeedPercent(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        double lhm = lhmFloatSensor("Control", "GPU Fan");
        if (lhm >= 0) {
            return lhm;
        }
        return lhmFloatSensor("Control", "GPU Fan 1");
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "GpuStats session has been closed. Obtain a new session via GraphicsCard.createStatsSession().");
        }
    }

    private long queryAdapterMemory(GpuAdapterMemoryProperty property) {
        if (luidPrefix.isEmpty()) {
            return -1L;
        }
        Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> adapterData = GpuInformation
                .queryGpuAdapterMemoryCounters();
        List<String> instances = adapterData.getA();
        List<Long> values = adapterData.getB().get(property);
        if (values != null) {
            String luidLower = luidPrefix.toLowerCase(Locale.ROOT);
            int limit = Math.min(instances.size(), values.size());
            for (int i = 0; i < limit; i++) {
                if (instances.get(i).toLowerCase(Locale.ROOT).contains(luidLower)) {
                    return values.get(i);
                }
            }
        }
        return -1L;
    }

    private String findNvmlDevice() {
        if (cachedNvmlDevice != null) {
            return cachedNvmlDevice.isEmpty() ? null : cachedNvmlDevice;
        }
        if (!NvmlUtil.isAvailable()) {
            cachedNvmlDevice = "";
            return null;
        }
        String id = null;
        if (!pciBusId.isEmpty()) {
            id = NvmlUtil.findDevice(pciBusId);
        }
        if (id == null) {
            id = NvmlUtil.findDeviceByName(cardName);
        }
        cachedNvmlDevice = id != null ? id : "";
        return id;
    }

    private int findAdlIndex() {
        if (cachedAdlIndex != Integer.MIN_VALUE) {
            return cachedAdlIndex;
        }
        if (!AdlUtil.isAvailable() || pciBusNumber < 0) {
            cachedAdlIndex = -1;
            return -1;
        }
        cachedAdlIndex = AdlUtil.findAdapterIndex(pciBusNumber);
        return cachedAdlIndex;
    }

    private double lhmFloatSensor(String sensorType, String sensorName) {
        if (lhmParent.isEmpty()) {
            return -1d;
        }
        try {
            WmiResult<LhmSensorProperty> sensors = LhmSensor.querySensors(lhmParent, sensorType);
            for (int i = 0; i < sensors.getResultCount(); i++) {
                if (sensorName.equals(WmiUtil.getString(sensors, LhmSensorProperty.NAME, i))) {
                    return WmiUtil.getFloat(sensors, LhmSensorProperty.VALUE, i);
                }
            }
        } catch (Exception e) {
            LOG.debug("LHM {} {} query failed: {}", sensorType, sensorName, e.getMessage());
        }
        return -1d;
    }
}
