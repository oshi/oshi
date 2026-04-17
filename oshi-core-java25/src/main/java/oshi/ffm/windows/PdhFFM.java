/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.MemoryLayout.structLayout;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public class PdhFFM extends WindowsForeignFunctions {

    private static final SymbolLookup Pdh = lib("Pdh");

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

    private static final MethodHandle PdhGetRawCounterValue = downcall(Pdh, "PdhGetRawCounterValue", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS);

    public static int PdhGetRawCounterValue(MemorySegment counter, MemorySegment type, MemorySegment value)
            throws Throwable {
        return (int) PdhGetRawCounterValue.invokeExact(counter, type, value);
    }

    // PDH_RAW_COUNTER: CStatus(4) + pad(4) + TimeStamp(8) + FirstValue(8) + SecondValue(8) + MultiCount(4) + pad(4)
    // MSVC aligns LONGLONG to 8 bytes, so 4 bytes padding after FILETIME (offset 12 → 16)
    public static final int PDH_CSTATUS_VALID_DATA = 0;
    public static final int PDH_CSTATUS_NEW_DATA = 1;

    public static final StructLayout PDH_RAW_COUNTER_LAYOUT = structLayout(JAVA_INT.withName("CStatus"),
            MemoryLayout.paddingLayout(4), JAVA_LONG.withName("TimeStamp"), JAVA_LONG.withName("FirstValue"),
            JAVA_LONG.withName("SecondValue"), JAVA_INT.withName("MultiCount"), MemoryLayout.paddingLayout(4));

    private static final MethodHandle PdhLookupPerfNameByIndexW = downcall(Pdh, "PdhLookupPerfNameByIndexW", JAVA_INT,
            ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static int PdhLookupPerfNameByIndexW(MemorySegment szMachineName, int dwNameIndex,
            MemorySegment szNameBuffer, MemorySegment pcchNameBufferSize) throws Throwable {
        return (int) PdhLookupPerfNameByIndexW.invokeExact(szMachineName, dwNameIndex, szNameBuffer,
                pcchNameBufferSize);
    }

    private static final MethodHandle PdhEnumObjectItemsW = downcall(Pdh, "PdhEnumObjectItemsW", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT);

    public static int PdhEnumObjectItemsW(MemorySegment szDataSource, MemorySegment szMachineName,
            MemorySegment szObjectName, MemorySegment mszCounterList, MemorySegment pcchCounterListLength,
            MemorySegment mszInstanceList, MemorySegment pcchInstanceListLength, int dwDetailLevel, int dwFlags)
            throws Throwable {
        return (int) PdhEnumObjectItemsW.invokeExact(szDataSource, szMachineName, szObjectName, mszCounterList,
                pcchCounterListLength, mszInstanceList, pcchInstanceListLength, dwDetailLevel, dwFlags);
    }

    private static final MethodHandle PdhCloseQuery = downcall(Pdh, "PdhCloseQuery", JAVA_INT, ADDRESS);

    public static int PdhCloseQuery(MemorySegment query) throws Throwable {
        return (int) PdhCloseQuery.invokeExact(query);
    }

}
