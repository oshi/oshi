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

public final class PdhUtilFFM {

    private PdhUtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(PdhUtilFFM.class);

    public static long getOpenFileDescriptors() {
        try (Arena arena = Arena.ofConfined()) {

            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = queryPtr.get(ADDRESS, 0);

            try {
                MemorySegment counterPtr = arena.allocate(ADDRESS);
                checkSuccess(PdhAddEnglishCounter(query, toWideString(arena, "\\Process(_Total)\\Handle Count"),
                        MemorySegment.NULL, counterPtr));
                MemorySegment counter = counterPtr.get(ADDRESS, 0);

                checkSuccess(PdhCollectQueryData(query));

                MemorySegment value = arena.allocate(PDH_FMT_COUNTERVALUE_LAYOUT);
                checkSuccess(PdhGetFormattedCounterValue(counter, PDH_FMT_LARGE, MemorySegment.NULL, value));

                long valueOffset = PDH_FMT_COUNTERVALUE_LAYOUT.byteOffset(groupElement("Value"),
                        groupElement("largeValue"));
                return value.get(JAVA_LONG, valueOffset);

            } finally {
                PdhCloseQuery(query);
            }

        } catch (Throwable t) {
            LOG.debug("PDH getOpenFileDescriptors failed: {}", t.getMessage(), t);
            return -1;
        }
    }

}
