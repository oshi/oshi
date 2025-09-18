/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class PsapiFFM extends WindowsForeignFunctions {

    private static final SymbolLookup PSAPI = lib("Psapi");

    private static final MethodHandle GetPerformanceInfo = downcall(PSAPI, "GetPerformanceInfo", JAVA_INT, ADDRESS,
            JAVA_INT);

    public static boolean GetPerformanceInfo(MemorySegment pPerfInfo, int size) throws Throwable {
        return isSuccess((int) GetPerformanceInfo.invokeExact(pPerfInfo, size));
    }
}
