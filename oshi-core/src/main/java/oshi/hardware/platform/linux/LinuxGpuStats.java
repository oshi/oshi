/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.io.File;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.gpu.NvmlUtil;

/**
 * Linux {@link GpuStats} session. Dynamic metrics are sourced from sysfs DRM driver files and NVML, using the same
 * priority ordering as {@link LinuxGraphicsCard}. The hwmon path and driver-specific sysfs paths are resolved once at
 * construction time.
 */
@ThreadSafe
final class LinuxGpuStats implements GpuStats {

    private final String drmDevicePath;
    private final String driverName;
    private final String pciBusId;
    private final String cardName;

    // Cached at construction; empty string if not found
    private final String hwmonPath;
    // Cached Intel gt0 path
    private final String gt0Path;

    private volatile boolean closed;

    LinuxGpuStats(String drmDevicePath, String driverName, String pciBusId, String cardName) {
        this.drmDevicePath = drmDevicePath;
        this.driverName = driverName;
        this.pciBusId = pciBusId;
        this.cardName = cardName;
        this.hwmonPath = resolveHwmonPath(drmDevicePath);
        this.gt0Path = drmDevicePath.isEmpty() ? "" : drmDevicePath + "/../gt/gt0";
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public GpuTicks getGpuTicks() {
        checkOpen();
        return new DefaultGpuTicks(System.nanoTime() / 100L, 0L);
    }

    @Override
    public double getGpuUtilization() {
        checkOpen();
        if (drmDevicePath.isEmpty()) {
            return -1d;
        }
        String driver = driverName.toLowerCase(Locale.ROOT);
        if ("amdgpu".equals(driver)) {
            int pct = FileUtil.getIntFromFile(drmDevicePath + "/gpu_busy_percent");
            return pct >= 0 ? pct : -1d;
        }
        if ("i915".equals(driver) || "xe".equals(driver)) {
            long actual = FileUtil.getLongFromFile(gt0Path + "/rps_act_freq_mhz");
            long max = FileUtil.getLongFromFile(gt0Path + "/rps_max_freq_mhz");
            if (actual > 0 && max > 0) {
                return Math.min(100.0, actual * 100.0 / max);
            }
        }
        return -1d;
    }

    @Override
    public long getVramUsed() {
        checkOpen();
        if (drmDevicePath.isEmpty()) {
            return -1L;
        }
        if ("amdgpu".equals(driverName.toLowerCase(Locale.ROOT))) {
            long used = FileUtil.getLongFromFile(drmDevicePath + "/mem_info_vram_used");
            return used >= 0 ? used : -1L;
        }
        return -1L;
    }

    @Override
    public long getSharedMemoryUsed() {
        checkOpen();
        return -1L;
    }

    @Override
    public double getTemperature() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getTemperature(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (!hwmonPath.isEmpty()) {
            long milliC = FileUtil.getLongFromFile(hwmonPath + "/temp1_input");
            if (milliC > 0) {
                return milliC / 1000.0;
            }
        }
        return -1d;
    }

    @Override
    public double getPowerDraw() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getPowerDraw(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (!hwmonPath.isEmpty()) {
            long microW = FileUtil.getLongFromFile(hwmonPath + "/power1_average");
            if (microW > 0) {
                return microW / 1_000_000.0;
            }
        }
        return -1d;
    }

    @Override
    public long getCoreClockMhz() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getCoreClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (drmDevicePath.isEmpty()) {
            return -1L;
        }
        String driver = driverName.toLowerCase(Locale.ROOT);
        if ("amdgpu".equals(driver)) {
            if (!hwmonPath.isEmpty()) {
                long hz = FileUtil.getLongFromFile(hwmonPath + "/freq1_input");
                if (hz > 0) {
                    return hz / 1_000_000L;
                }
            }
            return parseDpmActiveMhz(drmDevicePath + "/pp_dpm_sclk");
        }
        if ("i915".equals(driver) || "xe".equals(driver)) {
            long mhz = FileUtil.getLongFromFile(gt0Path + "/rps_cur_freq_mhz");
            return mhz > 0 ? mhz : -1L;
        }
        return -1L;
    }

    @Override
    public long getMemoryClockMhz() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            long val = NvmlUtil.getMemoryClockMhz(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (drmDevicePath.isEmpty()) {
            return -1L;
        }
        if ("amdgpu".equals(driverName.toLowerCase(Locale.ROOT))) {
            if (!hwmonPath.isEmpty()) {
                long hz = FileUtil.getLongFromFile(hwmonPath + "/freq2_input");
                if (hz > 0) {
                    return hz / 1_000_000L;
                }
            }
            return parseDpmActiveMhz(drmDevicePath + "/pp_dpm_mclk");
        }
        return -1L;
    }

    @Override
    public double getFanSpeedPercent() {
        checkOpen();
        String nvmlDevice = findNvmlDevice();
        if (nvmlDevice != null) {
            double val = NvmlUtil.getFanSpeedPercent(nvmlDevice);
            if (val >= 0) {
                return val;
            }
        }
        if (!hwmonPath.isEmpty()) {
            long fanRpm = FileUtil.getLongFromFile(hwmonPath + "/fan1_input");
            long fanMax = FileUtil.getLongFromFile(hwmonPath + "/fan1_max");
            if (fanRpm > 0 && fanMax > 0) {
                return Math.min(100.0, fanRpm * 100.0 / fanMax);
            }
            long pwm = FileUtil.getLongFromFile(hwmonPath + "/pwm1");
            if (pwm > 0) {
                return pwm / 255.0 * 100.0;
            }
        }
        return -1d;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("GpuStats session is closed");
        }
    }

    private String findNvmlDevice() {
        if (!NvmlUtil.isAvailable()) {
            return null;
        }
        if (!pciBusId.isEmpty()) {
            String id = NvmlUtil.findDevice(pciBusId);
            if (id != null) {
                return id;
            }
        }
        return NvmlUtil.findDeviceByName(cardName);
    }

    private static String resolveHwmonPath(String drmDevicePath) {
        if (drmDevicePath.isEmpty()) {
            return "";
        }
        File hwmonDir = new File(drmDevicePath + "/hwmon");
        File[] entries = hwmonDir.listFiles(f -> f.getName().startsWith("hwmon"));
        if (entries != null && entries.length > 0) {
            return entries[0].getAbsolutePath();
        }
        return "";
    }

    private static long parseDpmActiveMhz(String path) {
        for (String line : FileUtil.readFile(path, false)) {
            if (line.endsWith("*")) {
                int mhzIdx = line.toLowerCase(Locale.ROOT).indexOf("mhz");
                if (mhzIdx > 0) {
                    int start = line.lastIndexOf(' ', mhzIdx - 1);
                    if (start >= 0) {
                        return ParseUtil.parseLongOrDefault(line.substring(start + 1, mhzIdx), -1L);
                    }
                }
            }
        }
        return -1L;
    }
}
