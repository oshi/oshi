/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.Win32Exception;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static java.lang.foreign.MemoryLayout.PathElement.groupElement;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.PdhFFM.PdhCloseQuery;
import static oshi.ffm.windows.PdhFFM.PdhOpenQuery;
import static oshi.ffm.windows.PdhFFM.PdhAddEnglishCounter;
import static oshi.ffm.windows.PdhFFM.PdhCollectQueryData;
import static oshi.ffm.windows.PdhFFM.PdhGetFormattedCounterArray;
import static oshi.ffm.windows.PdhFFM.PDH_FMT_LARGE;
import static oshi.ffm.windows.PdhFFM.PDH_MORE_DATA;
import static oshi.ffm.windows.PdhFFM.PDH_FMT_COUNTERVALUE_ITEM_LAYOUT;
import static oshi.ffm.windows.WindowsForeignFunctions.checkSuccess;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

public class PdhUtilFFM {

    private PdhUtilFFM() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(PdhUtilFFM.class);

    public static long getOpenFileDescriptors() {
        long totalHandles = 0L;

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment queryPtr = arena.allocate(ADDRESS);
            checkSuccess(PdhOpenQuery(MemorySegment.NULL, MemorySegment.NULL, queryPtr));
            MemorySegment query = queryPtr.get(ADDRESS, 0);

            try {
                MemorySegment counterPtr = arena.allocate(ADDRESS);
                checkSuccess(PdhAddEnglishCounter(query, toWideString(arena, "\\Process(*)\\Handle Count"),
                        MemorySegment.NULL, counterPtr));
                MemorySegment counter = counterPtr.get(ADDRESS, 0);

                checkSuccess(PdhCollectQueryData(query));

                MemorySegment bufferSize = arena.allocate(JAVA_INT);
                MemorySegment itemCount = arena.allocate(JAVA_INT);

                int rc = PdhGetFormattedCounterArray(counter, PDH_FMT_LARGE, bufferSize, itemCount, MemorySegment.NULL);

                if (rc != PDH_MORE_DATA) {
                    throw new Win32Exception(rc);
                }

                MemorySegment buffer = arena.allocate(bufferSize.get(JAVA_INT, 0));

                checkSuccess(PdhGetFormattedCounterArray(counter, PDH_FMT_LARGE, bufferSize, itemCount, buffer));

                int count = itemCount.get(JAVA_INT, 0);
                long offset = 0;
                long itemSize = PDH_FMT_COUNTERVALUE_ITEM_LAYOUT.byteSize();

                for (int i = 0; i < count - 1; i++) {
                    MemorySegment item = buffer.asSlice(offset, itemSize);
                    long value = item.get(JAVA_LONG, PDH_FMT_COUNTERVALUE_ITEM_LAYOUT
                            .byteOffset(groupElement("FmtValue"), groupElement("Value"), groupElement("largeValue")));
                    totalHandles += value;
                    offset += itemSize;
                }

            } finally {
                PdhCloseQuery(query);
            }

        } catch (Throwable t) {
            LOG.debug("PDH getOpenFileDescriptors failed", t.getMessage());
            return -1;
        }

        return totalHandles;
    }

}
