/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFNumberRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.mac.IOKit.IOIterator;
import com.sun.jna.platform.mac.IOKit.IORegistryEntry;
import com.sun.jna.platform.mac.IOKitUtil;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GraphicsCard;
import oshi.hardware.GpuTicks;
import oshi.hardware.common.AbstractGraphicsCard;
import oshi.hardware.common.DefaultGpuTicks;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;

/**
 * Graphics card info obtained by system_profiler SPDisplaysDataType, with dynamic metrics from IOKit IOAccelerator
 * PerformanceStatistics.
 */
@ThreadSafe
final class MacGraphicsCard extends AbstractGraphicsCard {

    private static final Logger LOG = LoggerFactory.getLogger(MacGraphicsCard.class);

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;

    // IOAccelerator PerformanceStatistics dictionary keys
    private static final String PERF_STATS_KEY = "PerformanceStatistics";
    private static final String GPU_CORE_UTIL_KEY = "GPU Core Utilization";
    private static final String DEVICE_UTIL_KEY = "Device Utilization %";
    private static final String VRAM_USED_KEY = "vramUsedBytes";

    // Divisor for GPU Core Utilization raw value (unsigned 32-bit scaled to 0-100%)
    private static final double GPU_UTIL_DIVISOR = 0xFFFFFFFFL;

    /**
     * Constructor for MacGraphicsCard
     *
     * @param name        The name
     * @param deviceId    The device ID
     * @param vendor      The vendor
     * @param versionInfo The version info
     * @param vram        The VRAM
     */
    MacGraphicsCard(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    /**
     * public method used by {@link oshi.hardware.common.AbstractHardwareAbstractionLayer} to access the graphics cards.
     *
     * @return List of {@link oshi.hardware.platform.mac.MacGraphicsCard} objects.
     */
    public static List<GraphicsCard> getGraphicsCards() {
        List<GraphicsCard> cardList = new ArrayList<>();
        List<String> sp = ExecutingCommand.runNative("system_profiler SPDisplaysDataType");
        String name = Constants.UNKNOWN;
        String deviceId = Constants.UNKNOWN;
        String vendor = Constants.UNKNOWN;
        List<String> versionInfoList = new ArrayList<>();
        long vram = 0;
        int cardNum = 0;
        for (String line : sp) {
            String[] split = line.trim().split(":", 2);
            if (split.length == 2) {
                String prefix = split[0].toLowerCase(Locale.ROOT);
                if (prefix.equals("chipset model")) {
                    // Save previous card
                    if (cardNum++ > 0) {
                        cardList.add(new MacGraphicsCard(name, deviceId, vendor,
                                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList),
                                vram));
                        versionInfoList.clear();
                    }
                    name = split[1].trim();
                } else if (prefix.equals("device id")) {
                    deviceId = split[1].trim();
                } else if (prefix.equals("vendor")) {
                    vendor = split[1].trim();
                } else if (prefix.contains("version") || prefix.contains("revision")) {
                    versionInfoList.add(line.trim());
                } else if (prefix.startsWith("vram")) {
                    vram = ParseUtil.parseDecimalMemorySizeToBinary(split[1].trim());
                }
            }
        }
        cardList.add(new MacGraphicsCard(name, deviceId, vendor,
                versionInfoList.isEmpty() ? Constants.UNKNOWN : String.join(", ", versionInfoList), vram));
        return cardList;
    }

    // -------------------------------------------------------------------------
    // Dynamic metric implementations
    // -------------------------------------------------------------------------

    @Override
    public GpuTicks getGpuTicks() {
        // macOS does not expose cumulative GPU tick counters via IOKit.
        return new DefaultGpuTicks(System.nanoTime() / 100L, 0L);
    }

    @Override
    public double getGpuUtilization() {
        CFMutableDictionaryRef perfStats = queryPerfStats();
        if (perfStats == null) {
            return -1d;
        }
        try {
            // Try "GPU Core Utilization" first (scaled 0..0xFFFFFFFF)
            CFStringRef coreUtilKey = CFStringRef.createCFString(GPU_CORE_UTIL_KEY);
            Pointer result = perfStats.getValue(coreUtilKey);
            coreUtilKey.release();
            if (result != null) {
                CFNumberRef num = new CFNumberRef(result);
                long raw = num.longValue();
                return raw / GPU_UTIL_DIVISOR * 100.0;
            }
            // Fallback: "Device Utilization %" (already 0-100 integer on some drivers)
            CFStringRef devUtilKey = CFStringRef.createCFString(DEVICE_UTIL_KEY);
            result = perfStats.getValue(devUtilKey);
            devUtilKey.release();
            if (result != null) {
                CFNumberRef num = new CFNumberRef(result);
                return num.longValue();
            }
        } finally {
            perfStats.release();
        }
        return -1d;
    }

    @Override
    public long getVramUsed() {
        CFMutableDictionaryRef perfStats = queryPerfStats();
        if (perfStats == null) {
            return -1L;
        }
        try {
            CFStringRef vramKey = CFStringRef.createCFString(VRAM_USED_KEY);
            Pointer result = perfStats.getValue(vramKey);
            vramKey.release();
            if (result != null) {
                CFNumberRef num = new CFNumberRef(result);
                return num.longValue();
            }
        } finally {
            perfStats.release();
        }
        return -1L;
    }

    @Override
    public long getSharedMemoryUsed() {
        return -1L;
    }

    /**
     * Queries the IOAccelerator PerformanceStatistics dictionary for the GPU matching this card's name.
     *
     * <p>
     * The caller is responsible for releasing the returned dictionary.
     *
     * @return the PerformanceStatistics dictionary, or null if no matching service or key is found
     */
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
                try {
                    CFMutableDictionaryRef props = service.createCFProperties();
                    if (props != null) {
                        // Match by "model" property to this card's name
                        Pointer modelPtr = props.getValue(modelKey);
                        boolean matches = false;
                        if (modelPtr != null) {
                            CFStringRef modelRef = new CFStringRef(modelPtr);
                            String model = modelRef.stringValue();
                            matches = matchesName(model);
                        }
                        if (matches) {
                            Pointer statsPtr = props.getValue(perfStatsKey);
                            if (statsPtr != null) {
                                // Return the PerformanceStatistics sub-dictionary.
                                // We must retain it before releasing props.
                                CFMutableDictionaryRef stats = new CFMutableDictionaryRef();
                                stats.setPointer(statsPtr);
                                CF.CFRetain(stats);
                                props.release();
                                return stats;
                            }
                        }
                        props.release();
                    }
                } finally {
                    service.release();
                    service = iter.next();
                }
            }
        } finally {
            iter.release();
            perfStatsKey.release();
            modelKey.release();
        }
        return null;
    }

    /**
     * Returns true if the given IOAccelerator model string matches this card's name. Uses case-insensitive comparison
     * after stripping common trademark symbols.
     *
     * @param model the model string from the IOAccelerator registry entry
     * @return true if the model string matches this card's name
     */
    private boolean matchesName(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        String normModel = model.toLowerCase(Locale.ROOT).replace("(r)", "").replace("(tm)", "").trim();
        String normName = getName().toLowerCase(Locale.ROOT).replace("(r)", "").replace("(tm)", "").trim();
        return normModel.equals(normName) || normModel.contains(normName);
    }
}
