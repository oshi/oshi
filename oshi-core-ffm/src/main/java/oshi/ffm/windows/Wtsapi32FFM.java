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

    /** Pass to WTSEnumerateProcessesEx to enumerate all sessions. */
    public static final int WTS_ANY_SESSION = -2;

    /** Request WTS_PROCESS_INFO_EX structures from WTSEnumerateProcessesEx. */
    public static final int WTS_PROCESS_INFO_LEVEL_1 = 1;

    /** WTS_TYPE_CLASS value for WTSTypeProcessInfoLevel1, used with WTSFreeMemoryEx. */
    public static final int WTS_TYPE_PROCESS_INFO_LEVEL_1 = 1;

    // WTS_PROCESS_INFO_EXW struct layout (64-bit):
    // SessionId(4) ProcessId(4) pProcessName(ptr) pUserSid(ptr)
    // NumberOfThreads(4) HandleCount(4) PagefileUsage(4) PeakPagefileUsage(4)
    // WorkingSetSize(4) PeakWorkingSetSize(4) UserTime(8) KernelTime(8) = 64 bytes
    public static final long PROCESS_INFO_EX_SIZE = 64;
    public static final long PROCESS_INFO_EX_PROCESS_ID = 4;
    public static final long PROCESS_INFO_EX_PROCESS_NAME = 8;
    public static final long PROCESS_INFO_EX_NUMBER_OF_THREADS = 24;
    public static final long PROCESS_INFO_EX_HANDLE_COUNT = 28;
    public static final long PROCESS_INFO_EX_PAGEFILE_USAGE = 32;
    public static final long PROCESS_INFO_EX_USER_TIME = 48;
    public static final long PROCESS_INFO_EX_KERNEL_TIME = 56;

    private static final MethodHandle WTSEnumerateSessionsW = downcall(WTS, "WTSEnumerateSessionsW", JAVA_INT, ADDRESS,
            JAVA_INT, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean WTSEnumerateSessions(MemorySegment hServer, int reserved, int version,
            MemorySegment ppSessionInfo, MemorySegment pCount) throws Throwable {
        return isSuccess((int) WTSEnumerateSessionsW.invokeExact(hServer, reserved, version, ppSessionInfo, pCount));
    }

    private static final MethodHandle WTSEnumerateProcessesExW = downcall(WTS, "WTSEnumerateProcessesExW", JAVA_INT,
            ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean WTSEnumerateProcessesEx(MemorySegment hServer, MemorySegment pLevel, int sessionId,
            MemorySegment ppProcessInfo, MemorySegment pCount) throws Throwable {
        return isSuccess((int) WTSEnumerateProcessesExW.invokeExact(hServer, pLevel, sessionId, ppProcessInfo, pCount));
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

    private static final MethodHandle WTSFreeMemoryExW = downcall(WTS, "WTSFreeMemoryExW", JAVA_INT, JAVA_INT, ADDRESS,
            JAVA_INT);

    public static boolean WTSFreeMemoryEx(int wtsTypeClass, MemorySegment pMemory, int numberOfEntries)
            throws Throwable {
        return isSuccess((int) WTSFreeMemoryExW.invokeExact(wtsTypeClass, pMemory, numberOfEntries));
    }
}
