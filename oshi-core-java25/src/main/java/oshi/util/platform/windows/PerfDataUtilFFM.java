/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.windows.PdhFFM.PDH_FMT_COUNTERVALUE_LAYOUT;
import static oshi.ffm.windows.PdhFFM.PDH_FMT_LARGE;
import static oshi.ffm.windows.PdhFFM.PdhAddEnglishCounter;
import static oshi.ffm.windows.PdhFFM.PdhCloseQuery;
import static oshi.ffm.windows.PdhFFM.PdhCollectQueryData;
import static oshi.ffm.windows.PdhFFM.PdhGetFormattedCounterValue;
import static oshi.ffm.windows.PdhFFM.PdhOpenQuery;
import static oshi.ffm.windows.WindowsForeignFunctions.checkSuccess;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.EnumMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.driver.common.windows.perfmon.PdhCounterProperty;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup using the FFM API.
 */
public final class PerfDataUtilFFM {

    private PerfDataUtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtilFFM.class);

    private static final long VALUE_OFFSET = PDH_FMT_COUNTERVALUE_LAYOUT.byteOffset(groupElement("Value"),
            groupElement("largeValue"));

    /**
     * Query a single PDH counter value.
     *
     * @param object   The PDH object name (e.g., "Process")
     * @param instance The instance name (e.g., "_Total"), or null for no instance
     * @param counter  The counter name (e.g., "Handle Count")
     * @return The counter value, or -1 if the query failed
     */
    public static long queryCounter(String object, String instance, String counter) {
        String path = counterPath(object, instance, counter);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = null;

            try {
                query = queryPtr.get(ADDRESS, 0);
                MemorySegment counterHandle = addEnglishCounter(arena, query, path);

                checkSuccess(PdhCollectQueryData(query));

                return readCounterValue(arena, counterHandle);

            } finally {
                if (query != null) {
                    PdhCloseQuery(query);
                }
            }

        } catch (Throwable t) {
            LOG.debug("PDH queryCounter failed for {}: {}", path, t.getMessage(), t);
            return -1;
        }
    }

    /**
     * Query multiple PDH counter values in a single query, one per enum constant.
     *
     * @param <T>          An enum implementing {@link PdhCounterProperty}
     * @param propertyEnum The enum class whose constants define the counters to query
     * @param perfObject   The PDH object name (e.g., "Process")
     * @return An {@link EnumMap} of values indexed by enum constant, or an empty map on failure
     */
    public static <T extends Enum<T> & PdhCounterProperty> Map<T, Long> queryCounters(Class<T> propertyEnum,
            String perfObject) {
        T[] props = propertyEnum.getEnumConstants();
        EnumMap<T, Long> valueMap = new EnumMap<>(propertyEnum);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = null;

            try {
                query = queryPtr.get(ADDRESS, 0);

                // Add all counters to the single query
                EnumMap<T, MemorySegment> counterHandles = new EnumMap<>(propertyEnum);
                for (T prop : props) {
                    String path = counterPath(perfObject, prop.getInstance(), prop.getCounter());
                    counterHandles.put(prop, addEnglishCounter(arena, query, path));
                }

                // Collect once
                checkSuccess(PdhCollectQueryData(query));

                // Read all values
                for (T prop : props) {
                    valueMap.put(prop, readCounterValue(arena, counterHandles.get(prop)));
                }

            } finally {
                if (query != null) {
                    PdhCloseQuery(query);
                }
            }

        } catch (Throwable t) {
            LOG.debug("PDH queryCounters failed for {}: {}", perfObject, t.getMessage(), t);
            return new EnumMap<>(propertyEnum);
        }
        return valueMap;
    }

    private static String counterPath(String object, String instance, String counter) {
        StringBuilder path = new StringBuilder();
        path.append('\\').append(object);
        if (instance != null) {
            path.append('(').append(instance).append(')');
        }
        path.append('\\').append(counter);
        return path.toString();
    }

    private static MemorySegment addEnglishCounter(Arena arena, MemorySegment query, String path) throws Throwable {
        MemorySegment counterPtr = arena.allocate(ADDRESS);
        checkSuccess(PdhAddEnglishCounter(query, toWideString(arena, path), MemorySegment.NULL, counterPtr));
        return counterPtr.get(ADDRESS, 0);
    }

    private static long readCounterValue(Arena arena, MemorySegment counterHandle) throws Throwable {
        MemorySegment value = arena.allocate(PDH_FMT_COUNTERVALUE_LAYOUT);
        checkSuccess(PdhGetFormattedCounterValue(counterHandle, PDH_FMT_LARGE, MemorySegment.NULL, value));
        return value.get(JAVA_LONG, VALUE_OFFSET);
    }

}
