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

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuAdapterMemoryProperty;
import oshi.driver.common.windows.perfmon.GpuInformation.GpuEngineProperty;
import oshi.driver.common.windows.wmi.LhmSensor.LhmSensorProperty;
import oshi.driver.windows.perfmon.GpuInformationFFM;
import oshi.driver.windows.wmi.LhmSensorFFM;
import oshi.ffm.util.gpu.AdlUtilFFM;
import oshi.ffm.util.gpu.NvmlUtilFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;
import oshi.util.tuples.Pair;

/**
 * Windows {@link GpuStats} session using FFM.
 */
@ThreadSafe
final class WindowsGpuStatsFFM implements GpuStats {

    private static final String GPU_CORE = "GPU Core";

    private static final Logger LOG = LoggerFactory.getLogger(WindowsGpuStatsFFM.class);

    private static final long MB_TO_BYTES = 1_048_576L;

    private final String luidPrefix;
    private final String lhmParent;
    private final int pciBusNumber;
    private final String pciBusId;
    private final String cardName;

    private boolean closed;

    private String cachedNvmlDevice;
    private int cachedAdlIndex = Integer.MIN_VALUE;
    private GpuTicks prevUtilTicks;

    WindowsGpuStatsFFM(String luidPrefix, String lhmParent, int pciBusNumber, String pciBusId, String cardName) {
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
        Pair<List<String>, Map<GpuEngineProperty, List<Long>>> engineData = GpuInformationFFM.queryGpuEngineCounters();
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
        for (Map.Entry<String, Long> entry : activeByType.entrySet()) {
            totalActive += entry.getValue();
            totalBase += baseByType.getOrDefault(entry.getKey(), 0L);
        }
        long idle = totalBase >= totalActive ? totalBase - totalActive : 0L;
        return new GpuTicks(totalActive, idle);
    }

    @Override
    public synchronized double getGpuUtilization() {
        checkOpen();
        if (!lhmParent.isEmpty()) {
            try {
                WmiResult<LhmSensorProperty> sensors = LhmSensorFFM.querySensors(lhmParent, "Load");
                for (int i = 0; i < sensors.getResultCount(); i++) {
                    if (GPU_CORE.equals(WmiUtilFFM.getString(sensors, LhmSensorProperty.NAME, i))) {
                        return WmiUtilFFM.getFloat(sensors, LhmSensorProperty.VALUE, i);
                    }
                }
            } catch (Exception e) {
                LOG.debug("LHM GPU utilization query failed: {}", e.getMessage());
            }
        }
        GpuTicks curr = getGpuTicks();
        if (prevUtilTicks != null) {
            long dActive = curr.getActiveTicks() - prevUtilTicks.getActiveTicks();
            long dIdle = curr.getIdleTicks() - prevUtilTicks.getIdleTicks();
            if (dActive < 0 || dIdle < 0) {
                // Counter reset or empty snapshot; discard and wait for next sample
                prevUtilTicks = curr;
                return -1d;
            }
            long dTotal = dActive + dIdle;
            prevUtilTicks = curr;
            return dTotal > 0 ? dActive * 100.0 / dTotal : -1d;
        }
        prevUtilTicks = curr;
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
                WmiResult<LhmSensorProperty> sensors = LhmSensorFFM.querySensors(lhmParent, "SmallData");
                for (int i = 0; i < sensors.getResultCount(); i++) {
                    if ("GPU Memory Used".equals(WmiUtilFFM.getString(sensors, LhmSensorProperty.NAME, i))) {
                        float mb = WmiUtilFFM.getFloat(sensors, LhmSensorProperty.VALUE, i);
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
            double val = NvmlUtilFFM.getTemperature(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtilFFM.getTemperature(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        return lhmFloatSensor("Temperature", GPU_CORE);
    }

    @Override
    public synchronized double getPowerDraw() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtilFFM.getPowerDraw(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtilFFM.getPowerDraw(adlIndex);
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
            long val = NvmlUtilFFM.getCoreClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            long val = AdlUtilFFM.getCoreClockMhz(adlIndex);
            if (val >= 0) {
                return val;
            }
        }
        double lhm = lhmFloatSensor("Clock", GPU_CORE);
        return lhm >= 0 ? (long) lhm : -1L;
    }

    @Override
    public synchronized long getMemoryClockMhz() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtilFFM.getMemoryClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            long val = AdlUtilFFM.getMemoryClockMhz(adlIndex);
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
            double val = NvmlUtilFFM.getFanSpeedPercent(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        int adlIndex = findAdlIndex();
        if (adlIndex >= 0) {
            double val = AdlUtilFFM.getFanSpeedPercent(adlIndex);
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
        Pair<List<String>, Map<GpuAdapterMemoryProperty, List<Long>>> adapterData = GpuInformationFFM
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
        if (!NvmlUtilFFM.isAvailable()) {
            cachedNvmlDevice = "";
            return null;
        }
        String id = null;
        if (!pciBusId.isEmpty()) {
            id = NvmlUtilFFM.findDevice(pciBusId);
        }
        if (id == null) {
            id = NvmlUtilFFM.findDeviceByName(cardName);
        }
        cachedNvmlDevice = id != null ? id : "";
        return id;
    }

    private int findAdlIndex() {
        if (cachedAdlIndex != Integer.MIN_VALUE) {
            return cachedAdlIndex;
        }
        if (!AdlUtilFFM.isAvailable() || pciBusNumber < 0) {
            cachedAdlIndex = -1;
            return -1;
        }
        cachedAdlIndex = AdlUtilFFM.findAdapterIndex(pciBusNumber);
        return cachedAdlIndex;
    }

    private double lhmFloatSensor(String sensorType, String sensorName) {
        if (lhmParent.isEmpty()) {
            return -1d;
        }
        try {
            WmiResult<LhmSensorProperty> sensors = LhmSensorFFM.querySensors(lhmParent, sensorType);
            for (int i = 0; i < sensors.getResultCount(); i++) {
                if (sensorName.equals(WmiUtilFFM.getString(sensors, LhmSensorProperty.NAME, i))) {
                    return WmiUtilFFM.getFloat(sensors, LhmSensorProperty.VALUE, i);
                }
            }
        } catch (Exception e) {
            LOG.debug("LHM {} {} query failed: {}", sensorType, sensorName, e.getMessage());
        }
        return -1d;
    }
}
