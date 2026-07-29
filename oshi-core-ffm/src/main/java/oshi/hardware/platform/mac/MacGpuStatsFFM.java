/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.mac.IOReportClientFFM;
import oshi.ffm.platform.mac.CoreFoundation.CFDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import oshi.ffm.platform.mac.CoreFoundation.CFNumberRef;
import oshi.ffm.platform.mac.CoreFoundation.CFStringRef;
import oshi.ffm.platform.mac.CoreFoundationFunctions;
import oshi.ffm.platform.mac.IOKit.IOIterator;
import oshi.ffm.platform.mac.IOKit.IORegistryEntry;
import oshi.ffm.util.platform.mac.IOKitUtilFFM;
import oshi.ffm.util.platform.mac.SmcUtilFFM;
import oshi.hardware.common.platform.mac.MacGpuStats;

/**
 * macOS {@link oshi.hardware.GpuStats} session using FFM. All sampling logic lives in {@link MacGpuStats}; this class
 * supplies only the two native reads.
 */
@ThreadSafe
final class MacGpuStatsFFM extends MacGpuStats {

    private static final String PERF_STATS_KEY = "PerformanceStatistics";

    MacGpuStatsFFM(boolean isAppleSilicon, String cardName) {
        super(isAppleSilicon, cardName, IOReportClientFFM::create);
    }

    @Override
    protected Map<String, Long> queryPerfStats(String... keys) {
        CFMutableDictionaryRef perfStats = openPerfStats();
        if (perfStats == null) {
            return null; // NOSONAR squid:S1168 - null and empty are different answers; see the superclass javadoc
        }
        try (perfStats) {
            Map<String, Long> values = new HashMap<>();
            for (String key : keys) {
                CFStringRef cfKey = CFStringRef.createCFString(key);
                try (cfKey) {
                    MemorySegment result = perfStats.getValue(cfKey);
                    if (!result.equals(MemorySegment.NULL)) {
                        values.put(key, CFNumberRef.longValue(result));
                    }
                }
            }
            return values;
        }
    }

    @Override
    protected double queryGpuTemperatureFromSmc() {
        int conn = SmcUtilFFM.smcOpen();
        if (conn == 0) {
            return -1d;
        }
        try {
            return SmcUtilFFM.smcGetMaxTemperature(conn, SmcUtilFFM.getGpuTemperatureKeys());
        } finally {
            SmcUtilFFM.smcClose(conn);
        }
    }

    /**
     * Walks the IOAccelerator registry for this card's PerformanceStatistics dictionary.
     *
     * @return the retained dictionary, which the caller must close, or {@code null} if this card has no accelerator
     *         entry
     */
    private CFMutableDictionaryRef openPerfStats() {
        IOIterator iter = IOKitUtilFFM.getMatchingServices("IOAccelerator");
        if (iter == null) {
            return null;
        }
        CFStringRef perfStatsKey = CFStringRef.createCFString(PERF_STATS_KEY);
        CFStringRef modelKey = CFStringRef.createCFString("model");
        try (iter; perfStatsKey; modelKey) {
            IORegistryEntry service = iter.next();
            while (service != null) {
                CFMutableDictionaryRef result = null;
                try (IORegistryEntry current = service) {
                    MemorySegment propsSeg = current.createCFProperties();
                    if (!propsSeg.equals(MemorySegment.NULL)) {
                        CFDictionaryRef props = new CFDictionaryRef(propsSeg);
                        try (props) {
                            MemorySegment modelSeg = props.getValue(modelKey);
                            if (!modelSeg.equals(MemorySegment.NULL)
                                    && matchesName(CFStringRef.stringValue(modelSeg))) {
                                MemorySegment statsSeg = props.getValue(perfStatsKey);
                                if (!statsSeg.equals(MemorySegment.NULL)) {
                                    try {
                                        CoreFoundationFunctions.CFRetain(statsSeg);
                                    } catch (Throwable _) {
                                        // CFRetain declares throws Throwable; swallow to keep flow clean
                                    }
                                    result = new CFMutableDictionaryRef(statsSeg);
                                }
                            }
                        }
                    }
                }
                if (result != null) {
                    return result;
                }
                service = iter.next();
            }
        }
        return null;
    }
}
