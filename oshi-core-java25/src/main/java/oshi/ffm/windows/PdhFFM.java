/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

public class PdhFFM extends WindowsForeignFunctions {

    private static final SymbolLookup Pdh = lib("Pdh");

    public static final int PDH_FMT_LARGE = 0x00000400;
    public static final int PDH_MORE_DATA = 0x800007D2;

    private static final MethodHandle PdhOpenQuery = downcall(Pdh, "PdhOpenQueryW", JAVA_INT, ADDRESS, ADDRESS,
            ADDRESS);

    public static int PdhOpenQuery(MemorySegment dataSource, MemorySegment userData, MemorySegment query)
            throws Throwable {
        return (int) PdhOpenQuery.invokeExact(dataSource, userData, query);
    }

    private static final MethodHandle PdhAddEnglishCounter = downcall(Pdh, "PdhAddEnglishCounterW", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS, ADDRESS);

    public static int PdhAddEnglishCounter(MemorySegment query, MemorySegment counterPath, MemorySegment userData,
            MemorySegment counter) throws Throwable {
        return (int) PdhAddEnglishCounter.invokeExact(query, counterPath, userData, counter);
    }

    private static final MethodHandle PdhCollectQueryData = downcall(Pdh, "PdhCollectQueryData", JAVA_INT, ADDRESS);

    public static int PdhCollectQueryData(MemorySegment query) throws Throwable {
        return (int) PdhCollectQueryData.invokeExact(query);
    }

    private static final MethodHandle PdhGetFormattedCounterArray = downcall(Pdh, "PdhGetFormattedCounterArrayW",
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    public static int PdhGetFormattedCounterArray(MemorySegment counter, int format, MemorySegment bufferSize,
            MemorySegment itemCount, MemorySegment buffer) throws Throwable {
        return (int) PdhGetFormattedCounterArray.invokeExact(counter, format, bufferSize, itemCount, buffer);
    }

    private static final MethodHandle PdhCloseQuery = downcall(Pdh, "PdhCloseQuery", JAVA_INT, ADDRESS);

    public static int PdhCloseQuery(MemorySegment query) throws Throwable {
        return (int) PdhCloseQuery.invokeExact(query);
    }

    public static final MemoryLayout PDH_FMT_COUNTERVALUE_UNION = MemoryLayout.unionLayout(
            JAVA_INT.withName("longValue"), JAVA_DOUBLE.withName("doubleValue"), JAVA_LONG.withName("largeValue"),
            ADDRESS.withName("AnsiStringValue"), ADDRESS.withName("WideStringValue"));

    public static final StructLayout PDH_FMT_COUNTERVALUE_LAYOUT = MemoryLayout.structLayout(
            JAVA_INT.withName("CStatus"), MemoryLayout.paddingLayout(4), // align union to 8 bytes
            PDH_FMT_COUNTERVALUE_UNION.withName("Value"));

    public static final StructLayout PDH_FMT_COUNTERVALUE_ITEM_LAYOUT = structLayout(ADDRESS.withName("szName"),
            PDH_FMT_COUNTERVALUE_LAYOUT.withName("FmtValue"));

}
