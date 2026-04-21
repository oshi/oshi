/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public final class PsapiFFM extends WindowsForeignFunctions {

    private static final SymbolLookup PSAPI = lib("Psapi");

    private static final MethodHandle GetPerformanceInfo = downcall(PSAPI, "GetPerformanceInfo", JAVA_INT, ADDRESS,
            JAVA_INT);

    // PERFORMANCE_INFORMATION struct (64-bit): DWORD cb + 10 SIZE_T + 3 DWORD
    public static final StructLayout PERFORMANCE_INFORMATION_LAYOUT = MemoryLayout.structLayout(JAVA_INT.withName("cb"),
            MemoryLayout.paddingLayout(4), JAVA_LONG.withName("CommitTotal"), JAVA_LONG.withName("CommitLimit"),
            JAVA_LONG.withName("CommitPeak"), JAVA_LONG.withName("PhysicalTotal"),
            JAVA_LONG.withName("PhysicalAvailable"), JAVA_LONG.withName("SystemCache"),
            JAVA_LONG.withName("KernelTotal"), JAVA_LONG.withName("KernelPaged"), JAVA_LONG.withName("KernelNonpaged"),
            JAVA_LONG.withName("PageSize"), JAVA_INT.withName("HandleCount"), JAVA_INT.withName("ProcessCount"),
            JAVA_INT.withName("ThreadCount"), MemoryLayout.paddingLayout(4));

    public static boolean GetPerformanceInfo(MemorySegment pPerfInfo, int size) throws Throwable {
        return isSuccess((int) GetPerformanceInfo.invokeExact(pPerfInfo, size));
    }
}
