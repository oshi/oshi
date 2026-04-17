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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to centralize the boilerplate portions of PDH counter setup using the FFM API.
 */
public final class PerfDataUtilFFM {

    private PerfDataUtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(PerfDataUtilFFM.class);

    /**
     * Query a single PDH counter value.
     *
     * @param object   The PDH object name (e.g., "Process")
     * @param instance The instance name (e.g., "_Total"), or null for no instance
     * @param counter  The counter name (e.g., "Handle Count")
     * @return The counter value, or -1 if the query failed
     */
    public static long queryCounter(String object, String instance, String counter) {
        StringBuilder path = new StringBuilder();
        path.append('\\').append(object);
        if (instance != null) {
            path.append('(').append(instance).append(')');
        }
        path.append('\\').append(counter);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = null;

            try {
                query = queryPtr.get(ADDRESS, 0);
                MemorySegment counterPtr = arena.allocate(ADDRESS);
                checkSuccess(PdhAddEnglishCounter(query, toWideString(arena, path.toString()), MemorySegment.NULL,
                        counterPtr));
                MemorySegment counterHandle = counterPtr.get(ADDRESS, 0);

                checkSuccess(PdhCollectQueryData(query));

                MemorySegment value = arena.allocate(PDH_FMT_COUNTERVALUE_LAYOUT);
                checkSuccess(PdhGetFormattedCounterValue(counterHandle, PDH_FMT_LARGE, MemorySegment.NULL, value));

                long valueOffset = PDH_FMT_COUNTERVALUE_LAYOUT.byteOffset(groupElement("Value"),
                        groupElement("largeValue"));
                return value.get(JAVA_LONG, valueOffset);

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

}
