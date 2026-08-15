/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.IOKit.IOConnect;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.IOReportClient;
import oshi.hardware.common.platform.mac.MacGpuStats;
import oshi.util.platform.mac.SmcUtil;

/**
 * macOS {@link oshi.hardware.GpuStats} session. All sampling logic lives in {@link MacGpuStats}; this class supplies
 * only the two native reads.
 */
@ThreadSafe
final class MacGpuStatsJNA extends MacGpuStats {

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    private static final String PERF_STATS_KEY = "PerformanceStatistics";

    MacGpuStatsJNA(boolean isAppleSilicon, String cardName) {
        super(isAppleSilicon, cardName, IOReportClient::create);
    }

    @Override
    protected @Nullable Map<String, Long> queryPerfStats(String... keys) {
        CFMutableDictionaryRef perfStats = openPerfStats();
        if (perfStats == null) {
            return null; // NOSONAR java:S1168 - null and empty are different answers; see the superclass javadoc
        }
        try {
            Map<String, Long> values = new HashMap<>();
            for (String key : keys) {
                CFStringRef cfKey = CFStringRef.createCFString(key);
                Pointer result = perfStats.getValue(cfKey);
                cfKey.release();
                if (result != null) {
                    values.put(key, new CFNumberRef(result).longValue());
                }
            }
            return values;
        } finally {
            perfStats.release();
        }
    }

    @Override
    protected double queryGpuTemperatureFromSmc() {
        IOConnect conn = SmcUtil.smcOpen();
        if (conn == null) {
            return -1d;
        }
        try {
            return SmcUtil.smcGetMaxTemperature(conn, SmcUtil.getGpuTemperatureKeys());
        } finally {
            SmcUtil.smcClose(conn);
        }
    }

    /**
     * Walks the IOAccelerator registry for this card's PerformanceStatistics dictionary.
     *
     * @return the retained dictionary, which the caller must release, or {@code null} if this card has no accelerator
     *         entry
     */
    private @Nullable CFMutableDictionaryRef openPerfStats() {
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
}
