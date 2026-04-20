/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public final class Wtsapi32FFM extends WindowsForeignFunctions {

    private static final SymbolLookup WTS = lib("Wtsapi32");

    public static final MemorySegment WTS_CURRENT_SERVER_HANDLE = MemorySegment.NULL;

    private static final MethodHandle WTSEnumerateSessionsW = downcall(WTS, "WTSEnumerateSessionsW", JAVA_INT, ADDRESS,
            JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean WTSEnumerateSessions(MemorySegment hServer, int reserved, int version,
            MemorySegment ppSessionInfo, MemorySegment pCount) throws Throwable {
        return isSuccess((int) WTSEnumerateSessionsW.invokeExact(hServer, reserved, version, ppSessionInfo, pCount));
    }

    private static final MethodHandle WTSQuerySessionInformationW = downcall(WTS, "WTSQuerySessionInformationW",
            JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean WTSQuerySessionInformation(MemorySegment hServer, int sessionId, int wtsInfoClass,
            MemorySegment ppBuffer, MemorySegment pBytesReturned) throws Throwable {
        return isSuccess((int) WTSQuerySessionInformationW.invokeExact(hServer, sessionId, wtsInfoClass, ppBuffer,
                pBytesReturned));
    }

    private static final MethodHandle WTSFreeMemory = downcall(WTS, "WTSFreeMemory", null, ADDRESS);

    public static void WTSFreeMemory(MemorySegment pMemory) throws Throwable {
        WTSFreeMemory.invokeExact(pMemory);
    }
}
