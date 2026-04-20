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

public final class Netapi32FFM extends WindowsForeignFunctions {

    private static final SymbolLookup NET = lib("Netapi32");

    public static final int MAX_PREFERRED_LENGTH = -1;

    private static final MethodHandle NetSessionEnum = downcall(NET, "NetSessionEnum", JAVA_INT, ADDRESS, ADDRESS,
            ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS);

    public static int NetSessionEnum(MemorySegment serverName, MemorySegment uncClientName, MemorySegment userName,
            int level, MemorySegment bufptr, int prefmaxlen, MemorySegment entriesRead, MemorySegment totalEntries,
            MemorySegment resumeHandle) throws Throwable {
        return (int) NetSessionEnum.invokeExact(serverName, uncClientName, userName, level, bufptr, prefmaxlen,
                entriesRead, totalEntries, resumeHandle);
    }

    private static final MethodHandle NetApiBufferFree = downcall(NET, "NetApiBufferFree", JAVA_INT, ADDRESS);

    public static int NetApiBufferFree(MemorySegment buffer) throws Throwable {
        return (int) NetApiBufferFree.invokeExact(buffer);
    }
}
