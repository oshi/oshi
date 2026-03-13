/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.IOKit.IOConnect;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.IOReportClient;
import oshi.hardware.GpuStats;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.util.platform.mac.SmcUtil;

/**
 * macOS {@link GpuStats} session. On Apple Silicon, GPU ticks, utilization, and power are sourced from an
 * {@link IOReportClient} subscription. Temperature is read from SMC. VRAM used and utilization fallback use the
 * IOAccelerator PerformanceStatistics dictionary (both Apple Silicon and Intel).
 */
@ThreadSafe
final class MacGpuStats implements GpuStats {

    private static final Logger LOG = LoggerFactory.getLogger(MacGpuStats.class);
    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private static final String PERF_STATS_KEY = "PerformanceStatistics";
    private static final String GPU_CORE_UTIL_KEY = "GPU Core Utilization";
    private static final String DEVICE_UTIL_KEY = "Device Utilization %";
    private static final String VRAM_USED_KEY = "vramUsedBytes";
    private static final String VRAM_USED_KEY_AS = "In use system memory";
    private static final double GPU_UTIL_DIVISOR = 0xFFFFFFFFL;
    private static final Pattern TRADEMARK_PATTERN = Pattern.compile("[®™]|\\([Rr]\\)|\\([Tt][Mm]\\)");

    private final boolean isAppleSilicon;
    private final String cardName;

    // Non-null only on Apple Silicon
    private final IOReportClient ioReportClient;

    private boolean closed;

    MacGpuStats(boolean isAppleSilicon, String cardName) {
        this.isAppleSilicon = isAppleSilicon;
        this.cardName = cardName;
        this.ioReportClient = isAppleSilicon ? IOReportClient.create() : null;
        if (isAppleSilicon && ioReportClient == null) {
            LOG.warn("IOReport subscription failed for '{}'; GPU ticks and power will be unavailable."
                    + " Utilization will fall back to IOAccelerator PerformanceStatistics.", cardName);
        }
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
        if (isAppleSilicon && ioReportClient != null) {
            return ioReportClient.sampleGpuTicks();
        }
        return new DefaultGpuTicks(System.nanoTime() / 100L, -1L);
    }

    @Override
    public synchronized double getGpuUtilization() {
        checkOpen();
        if (isAppleSilicon && ioReportClient != null) {
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
            Pointer result = perfStats.getValue(coreUtilKey);
            coreUtilKey.release();
            if (result != null) {
                return new CFNumberRef(result).longValue() / GPU_UTIL_DIVISOR * 100.0;
            }
            CFStringRef devUtilKey = CFStringRef.createCFString(DEVICE_UTIL_KEY);
            result = perfStats.getValue(devUtilKey);
            devUtilKey.release();
            if (result != null) {
                return new CFNumberRef(result).longValue();
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
            Pointer result = perfStats.getValue(key);
            key.release();
            if (result != null) {
                return new CFNumberRef(result).longValue();
            }
            CFStringRef fallback = CFStringRef.createCFString(fallbackKey);
            result = perfStats.getValue(fallback);
            fallback.release();
            if (result != null) {
                return new CFNumberRef(result).longValue();
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
            IOConnect conn = SmcUtil.smcOpen();
            if (conn != null) {
                try {
                    double temp = SmcUtil.smcGetFirstFloat(conn, SmcUtil.SMC_KEYS_GPU_TEMP_AS);
                    if (temp > 0) {
                        return temp;
                    }
                } finally {
                    SmcUtil.smcClose(conn);
                }
            }
        }
        CFMutableDictionaryRef perfStats = queryPerfStats();
        if (perfStats == null) {
            return -1d;
        }
        try {
            CFStringRef tempKey = CFStringRef.createCFString("Temperature(C)");
            Pointer result = perfStats.getValue(tempKey);
            tempKey.release();
            if (result != null) {
                long val = new CFNumberRef(result).longValue();
                if (val > 0) {
                    return val;
                }
            }
        } finally {
            perfStats.release();
        }
        return -1d;
    }

    @Override
    public synchronized double getPowerDraw() {
        checkOpen();
        if (isAppleSilicon && ioReportClient != null) {
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
        IOIterator iter = IOKitUtil.getMatchingServices("IOAccelerator");
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
                    CFMutableDictionaryRef props = service.createCFProperties();
                    if (props != null) {
                        try {
                            Pointer modelPtr = props.getValue(modelKey);
                            if (modelPtr != null && matchesName(new CFStringRef(modelPtr).stringValue())) {
                                Pointer statsPtr = props.getValue(perfStatsKey);
                                if (statsPtr != null) {
                                    CFMutableDictionaryRef stats = new CFMutableDictionaryRef();
                                    stats.setPointer(statsPtr);
                                    CF.CFRetain(stats);
                                    result = stats;
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
        String normName = TRADEMARK_PATTERN.matcher(cardName.toLowerCase(Locale.ROOT)).replaceAll("").trim();
        if (normModel.equals(normName)) {
            return true;
        }
        Matcher m = Pattern.compile("\\b" + Pattern.quote(normName) + "\\b").matcher(normModel);
        return m.find();
    }
}
