/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.lang.foreign.MemorySegment;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.IOReportClientFFM;
import oshi.ffm.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.mac.CoreFoundation.CFStringRef;
import oshi.ffm.mac.CoreFoundationFunctions;
import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;
import oshi.util.platform.mac.IOKitUtilFFM;
import oshi.util.platform.mac.SmcUtilFFM;

/**
 * macOS {@link GpuStats} session using FFM.
 *
 * <p>
 * On Apple Silicon, GPU ticks, utilization, and power via IOReport are not yet available in the FFM path; utilization
 * falls back to IOAccelerator PerformanceStatistics. Temperature is read from SMC first, then falls back to
 * IOAccelerator {@code Temperature(C)}.
 *
 * <p>
 * On Intel Mac, utilization and VRAM used are sourced from IOAccelerator PerformanceStatistics.
 *
 * <p>
 * Clock speeds, fan speed, and shared memory are not available on any macOS path and always return -1.
 */
@ThreadSafe
final class MacGpuStatsFFM implements GpuStats {

    private static final Logger LOG = LoggerFactory.getLogger(MacGpuStatsFFM.class);

    private static final String PERF_STATS_KEY = "PerformanceStatistics";
    private static final String GPU_CORE_UTIL_KEY = "GPU Core Utilization";
    private static final String DEVICE_UTIL_KEY = "Device Utilization %";
    private static final String VRAM_USED_KEY = "vramUsedBytes";
    private static final String VRAM_USED_KEY_AS = "In use system memory";
    private static final double GPU_UTIL_DIVISOR = 0xFFFFFFFFL;
    private static final Pattern TRADEMARK_PATTERN = Pattern.compile("[®™]|\\([Rr]\\)|\\([Tt][Mm]\\)");

    private final boolean isAppleSilicon;
    private final String cardName;
    private final IOReportClientFFM ioReportClient;
    private final String normCardName;
    private final Pattern cardNamePattern;

    private boolean closed;

    MacGpuStatsFFM(boolean isAppleSilicon, String cardName) {
        this.isAppleSilicon = isAppleSilicon;
        this.cardName = cardName;
        this.ioReportClient = IOReportClientFFM.create();
        this.normCardName = TRADEMARK_PATTERN.matcher(cardName.toLowerCase(Locale.ROOT)).replaceAll("").trim();
        this.cardNamePattern = Pattern.compile("\\b" + Pattern.quote(normCardName) + "\\b");
    }

    @Override
    public synchronized void close() {
        closed = true;
        if (ioReportClient != null) {
            ioReportClient.close();
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized GpuTicks getGpuTicks() {
        checkOpen();
        if (ioReportClient != null) {
            return ioReportClient.sampleGpuTicks();
        }
        return new GpuTicks(0L, 0L);
    }

    @Override
    public synchronized double getGpuUtilization() {
        checkOpen();
        if (ioReportClient != null) {
            double util = ioReportClient.sampleGpuUtilization();
            if (util >= 0) {
                return util;
            }
        }
        CFMutableDictionaryRef perfStats = queryPerfStats();
        if (perfStats == null) {
            return -1d;
        }
        try {
            CFStringRef coreUtilKey = CFStringRef.createCFString(GPU_CORE_UTIL_KEY);
            try {
                MemorySegment result = perfStats.getValue(coreUtilKey);
                if (!result.equals(MemorySegment.NULL)) {
                    return new CFNumberRef(result).longValue() / GPU_UTIL_DIVISOR * 100.0;
                }
            } finally {
                coreUtilKey.release();
            }
            CFStringRef devUtilKey = CFStringRef.createCFString(DEVICE_UTIL_KEY);
            try {
                MemorySegment result = perfStats.getValue(devUtilKey);
                if (!result.equals(MemorySegment.NULL)) {
                    return new CFNumberRef(result).longValue();
                }
            } finally {
                devUtilKey.release();
            }
        } finally {
            perfStats.release();
        }
        return -1d;
    }

    @Override
    public synchronized long getVramUsed() {
        checkOpen();
        CFMutableDictionaryRef perfStats = queryPerfStats();
        if (perfStats == null) {
            return -1L;
        }
        try {
            String primaryKey = isAppleSilicon ? VRAM_USED_KEY_AS : VRAM_USED_KEY;
            String fallbackKey = isAppleSilicon ? VRAM_USED_KEY : VRAM_USED_KEY_AS;
            CFStringRef key = CFStringRef.createCFString(primaryKey);
            try {
                MemorySegment result = perfStats.getValue(key);
                if (!result.equals(MemorySegment.NULL)) {
                    return new CFNumberRef(result).longValue();
                }
            } finally {
                key.release();
            }
            CFStringRef fallback = CFStringRef.createCFString(fallbackKey);
            try {
                MemorySegment result = perfStats.getValue(fallback);
                if (!result.equals(MemorySegment.NULL)) {
                    return new CFNumberRef(result).longValue();
                }
            } finally {
                fallback.release();
            }
        } finally {
            perfStats.release();
        }
        return -1L;
    }

    @Override
    public synchronized long getSharedMemoryUsed() {
        checkOpen();
        return -1L;
    }

    @Override
    public synchronized double getTemperature() {
        checkOpen();
        if (isAppleSilicon) {
            int conn = SmcUtilFFM.smcOpen();
            if (conn != 0) {
                try {
                    double temp = SmcUtilFFM.smcGetFirstFloat(conn, SmcUtilFFM.SMC_KEYS_GPU_TEMP_AS);
                    if (temp > 0) {
                        return temp;
                    }
                } finally {
                    SmcUtilFFM.smcClose(conn);
                }
            }
        }
        CFMutableDictionaryRef perfStats = queryPerfStats();
        if (perfStats == null) {
            return -1d;
        }
        try {
            CFStringRef tempKey = CFStringRef.createCFString("Temperature(C)");
            try {
                MemorySegment result = perfStats.getValue(tempKey);
                if (!result.equals(MemorySegment.NULL)) {
                    long val = new CFNumberRef(result).longValue();
                    if (val > 0) {
                        return val;
                    }
                }
            } finally {
                tempKey.release();
            }
        } finally {
            perfStats.release();
        }
        return -1d;
    }

    @Override
    public synchronized double getPowerDraw() {
        checkOpen();
        if (ioReportClient != null) {
            return ioReportClient.samplePowerWatts();
        }
        return -1d;
    }

    @Override
    public synchronized long getCoreClockMhz() {
        checkOpen();
        return -1L;
    }

    @Override
    public synchronized long getMemoryClockMhz() {
        checkOpen();
        return -1L;
    }

    @Override
    public synchronized double getFanSpeedPercent() {
        checkOpen();
        return -1d;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "GpuStats session has been closed. Obtain a new session via GraphicsCard.createStatsSession().");
        }
    }

    private CFMutableDictionaryRef queryPerfStats() {
        IOIterator iter = IOKitUtilFFM.getMatchingServices("IOAccelerator");
        if (iter == null) {
            return null;
        }
        CFStringRef perfStatsKey = CFStringRef.createCFString(PERF_STATS_KEY);
        CFStringRef modelKey = CFStringRef.createCFString("model");
        try {
            IORegistryEntry service = iter.next();
            while (service != null) {
                CFMutableDictionaryRef result = null;
                try {
                    MemorySegment propsSeg = service.createCFProperties();
                    if (!propsSeg.equals(MemorySegment.NULL)) {
                        CFDictionaryRef props = new CFDictionaryRef(propsSeg);
                        try {
                            MemorySegment modelSeg = props.getValue(modelKey);
                            if (!modelSeg.equals(MemorySegment.NULL)) {
                                String model = new CFStringRef(modelSeg).stringValue();
                                if (matchesName(model)) {
                                    MemorySegment statsSeg = props.getValue(perfStatsKey);
                                    if (!statsSeg.equals(MemorySegment.NULL)) {
                                        try {
                                            CoreFoundationFunctions.CFRetain(statsSeg);
                                        } catch (Throwable ignored) {
                                            // CFRetain declares throws Throwable; swallow to keep flow clean
                                        }
                                        result = new CFMutableDictionaryRef(statsSeg);
                                    }
                                }
                            }
                        } finally {
                            props.release();
                        }
                    }
                } finally {
                    service.release();
                }
                if (result != null) {
                    return result;
                }
                service = iter.next();
            }
        } finally {
            iter.release();
            perfStatsKey.release();
            modelKey.release();
        }
        return null;
    }

    private boolean matchesName(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        String normModel = TRADEMARK_PATTERN.matcher(model.toLowerCase(Locale.ROOT)).replaceAll("").trim();
        if (normModel.equals(normCardName)) {
            return true;
        }
        return cardNamePattern.matcher(normModel).find();
    }
}
