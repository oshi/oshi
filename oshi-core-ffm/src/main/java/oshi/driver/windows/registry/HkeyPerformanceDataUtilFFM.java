/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.registry;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.platform.windows.Advapi32FFM.RegQueryValueEx;
import static oshi.ffm.platform.windows.WinErrorFFM.ERROR_MORE_DATA;
import static oshi.ffm.platform.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.readWideString;
import static oshi.ffm.platform.windows.WindowsForeignFunctions.toWideString;
import static oshi.util.ExceptionUtil.getOrDefault;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.perfmon.PdhCounterWildcardProperty;
import oshi.driver.common.windows.registry.HkeyPerformanceDataUtil;
import oshi.driver.common.windows.registry.PerfDataBuffer;
import oshi.ffm.platform.windows.WinRegFFM;
import oshi.ffm.util.platform.windows.Advapi32UtilFFM;
import oshi.util.tuples.Pair;
import oshi.util.tuples.Triplet;

/**
 * Utility to read HKEY_PERFORMANCE_DATA information using the FFM API.
 */
@ThreadSafe
public final class HkeyPerformanceDataUtilFFM extends HkeyPerformanceDataUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HkeyPerformanceDataUtilFFM.class);

    /*
     * Do a one-time lookup of the HKEY_PERFORMANCE_TEXT counter indices and store in a map for efficient lookups
     * on-demand.
     */
    private static final Map<String, Integer> COUNTER_INDEX_MAP = mapCounterIndicesFromRegistry();

    private static int maxPerfBufferSize = 16384;

    private HkeyPerformanceDataUtilFFM() {
    }

    /**
     * Looks up the performance counter index for the given English counter name.
     *
     * @param name The English counter name
     * @return The counter index, or 0 if not found
     */
    public static int getCounterIndex(String name) {
        return COUNTER_INDEX_MAP.getOrDefault(name, 0);
    }

    /**
     * Reads and parses a block of performance data from the registry.
     *
     * @param <T>         PDH Counters use an Enum to identify the fields to query in either the counter or WMI backup,
     *                    and use the enum values as keys to retrieve the results.
     * @param objectName  The counter object for which to fetch data
     * @param counterEnum Which counters to return data for
     * @return A triplet containing the results. The first element maps the input enum to the counter values where the
     *         first enum will contain the instance name as a {@link String}, and the remaining values will either be
     *         {@link Long}, {@link Integer}, or {@code null} depending on whether the specified enum counter was
     *         present and the size of the counter value, or {@code null} if the object could not be read. The second
     *         element is a timestamp in 100nSec increments (Windows 1601 Epoch) while the third element is a timestamp
     *         in milliseconds since the 1970 Epoch.
     */
    public static <T extends Enum<T> & PdhCounterWildcardProperty> @Nullable Triplet<List<Map<T, Object>>, Long, Long> readPerfDataFromRegistry(
            String objectName, Class<T> counterEnum) {
        // Load indices
        // e.g., call with "Process" and ProcessPerformanceProperty.class
        Pair<Integer, EnumMap<T, Integer>> indices = getCounterIndices(objectName, counterEnum, COUNTER_INDEX_MAP);
        if (indices == null) {
            return null;
        }
        int objectIndex = indices.getA();
        EnumMap<T, Integer> enumIndexMap = indices.getB();
        // The above test checks validity of objectName as an index but it could still
        // fail to read
        MemorySegment pPerfData = readPerfDataBuffer(objectName);
        if (pPerfData == null) {
            return null;
        }
        return parsePerfData(new SegmentPerfDataBuffer(pPerfData), objectIndex, enumIndexMap, counterEnum);
    }

    /**
     * Reads the performance data block through an FFM {@link MemorySegment}.
     *
     * @param segment The populated performance data block
     */
    private record SegmentPerfDataBuffer(MemorySegment segment) implements PerfDataBuffer {

        @Override
        public int getInt(long offset) {
            return this.segment.get(JAVA_INT, offset);
        }

        @Override
        public long getLong(long offset) {
            return this.segment.get(JAVA_LONG, offset);
        }

        @Override
        public String getWideString(long offset) {
            return readWideString(this.segment.asSlice(offset));
        }
    }

    /**
     * Read the performance data for a counter object from the registry.
     *
     * @param objectName The counter object for which to fetch data. It is the user's responsibility to ensure this key
     *                   exists in {@link #COUNTER_INDEX_MAP}.
     * @return A buffer containing the data if successful, null otherwise.
     */
    private static synchronized @Nullable MemorySegment readPerfDataBuffer(String objectName) {
        // Need this index as a string
        Integer objectIndex = COUNTER_INDEX_MAP.get(objectName);
        if (objectIndex == null) {
            LOG.error("No counter index for performance object {}.", objectName);
            return null;
        }
        String objectIndexStr = objectIndex.toString();

        // Now load the data from the registry.
        // Use a global arena so the returned segment outlives this method
        Arena arena = Arena.ofAuto();
        return getOrDefault(() -> {
            MemorySegment lpValueName = toWideString(arena, objectIndexStr);
            MemorySegment lpcbData = arena.allocate(JAVA_INT);
            lpcbData.set(JAVA_INT, 0, maxPerfBufferSize);
            MemorySegment pPerfData = arena.allocate(maxPerfBufferSize);

            int ret = RegQueryValueEx(MemorySegment.ofAddress(WinRegFFM.HKEY_PERFORMANCE_DATA), lpValueName, 0,
                    MemorySegment.NULL, pPerfData, lpcbData);
            if (ret != ERROR_SUCCESS && ret != ERROR_MORE_DATA) {
                LOG.error("Error reading performance data from registry for {}.", objectName);
                return null;
            }
            // Grow buffer as needed to fit the data
            while (ret == ERROR_MORE_DATA) {
                maxPerfBufferSize += 8192;
                lpcbData.set(JAVA_INT, 0, maxPerfBufferSize);
                pPerfData = arena.allocate(maxPerfBufferSize);
                ret = RegQueryValueEx(MemorySegment.ofAddress(WinRegFFM.HKEY_PERFORMANCE_DATA), lpValueName, 0,
                        MemorySegment.NULL, pPerfData, lpcbData);
            }
            if (ret != ERROR_SUCCESS) {
                LOG.error("Error reading performance data from registry for {} (ret={}).", objectName, ret);
                return null;
            }
            return pPerfData;
        }, null, LOG, "Error reading performance data from registry for {}.", objectName);
    }

    /**
     * Registry entries subordinate to HKEY_PERFORMANCE_TEXT key reference the text strings that describe counters in US
     * English. Not supported in Windows 2000.
     *
     * With the "Counter" value, the resulting array contains alternating index/name pairs "1", "1847", "2", "System",
     * "4", "Memory", ...
     *
     * These pairs are translated to a map for later lookup.
     *
     * @return An unmodifiable map containing counter name strings as keys and indices as integer values if the key is
     *         read successfully; an empty map otherwise.
     */
    private static Map<String, Integer> mapCounterIndicesFromRegistry() {
        try {
            String[] counterText = Advapi32UtilFFM.registryGetStringArray(
                    MemorySegment.ofAddress(WinRegFFM.HKEY_LOCAL_MACHINE), HKEY_PERFORMANCE_TEXT, COUNTER);
            return buildCounterIndexMap(counterText);
        } catch (Exception e) {
            LOG.error(
                    "Unable to locate English counter names in registry Perflib 009. Counters may need to be rebuilt: ",
                    e);
        }
        return buildCounterIndexMap(null);
    }
}
