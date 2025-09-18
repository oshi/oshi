/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class Advapi32FFM extends WindowsForeignFunctions {

    private static final SymbolLookup ADV = lib("Advapi32");

    private static final MethodHandle AdjustTokenPrivileges = downcall(ADV, "AdjustTokenPrivileges", JAVA_INT, ADDRESS,
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean AdjustTokenPrivileges(MemorySegment hToken, MemorySegment tkp) throws Throwable {
        return isSuccess(
                (int) AdjustTokenPrivileges.invokeExact(hToken, 0, tkp, 0, MemorySegment.NULL, MemorySegment.NULL));
    }

    private static final MethodHandle CloseEventLog = downcall(ADV, "CloseEventLog", JAVA_INT, ADDRESS);

    public static boolean CloseEventLog(MemorySegment hEventLog) throws Throwable {
        return isSuccess((int) CloseEventLog.invokeExact(hEventLog));
    }

    private static final MethodHandle GetTokenInformation = downcall(ADV, "GetTokenInformation", JAVA_INT, ADDRESS,
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);

    public static boolean GetTokenInformation(MemorySegment hToken, int tokenInfoClass, MemorySegment tokenInfo,
            int tokenInfoLength, MemorySegment returnLength) throws Throwable {
        return isSuccess((int) GetTokenInformation.invokeExact(hToken, tokenInfoClass, tokenInfo, tokenInfoLength,
                returnLength));
    }

    private static final MethodHandle LookupPrivilegeValue = downcall(ADV, "LookupPrivilegeValueW", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS);

    public static boolean LookupPrivilegeValue(String name, MemorySegment luid, Arena arena) throws Throwable {
        MemorySegment nameSeg = toWideString(arena, name);
        return isSuccess((int) LookupPrivilegeValue.invokeExact(MemorySegment.NULL, nameSeg, luid));
    }

    private static final MethodHandle OpenEventLog = downcall(ADV, "OpenEventLogW", ADDRESS, ADDRESS, ADDRESS);

    public static Optional<MemorySegment> OpenEventLog(MemorySegment serverName, MemorySegment sourceName)
            throws Throwable {
        MemorySegment handle = (MemorySegment) OpenEventLog.invokeExact(serverName, sourceName);
        if (handle == null || handle.equals(MemorySegment.NULL)) {
            return Optional.empty();
        }
        return Optional.of(handle);
    }

    public static Optional<MemorySegment> OpenEventLog(Arena arena, String source) {
        try {
            MemorySegment lpSource = WindowsForeignFunctions.toWideString(arena, source);
            MemorySegment handle = (MemorySegment) OpenEventLog.invokeExact(MemorySegment.NULL, lpSource);
            if (handle.address() == 0) {
                return Optional.empty();
            }
            return Optional.of(handle);
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    private static final MethodHandle OpenProcessToken = downcall(ADV, "OpenProcessToken", JAVA_INT, ADDRESS, JAVA_INT,
            ADDRESS);

    public static boolean OpenProcessToken(MemorySegment process, int desiredAccess, MemorySegment hTokenOut)
            throws Throwable {
        return isSuccess((int) OpenProcessToken.invokeExact(process, desiredAccess, hTokenOut));
    }

    private static final MethodHandle ReadEventLog = downcall(ADV, "ReadEventLogW", JAVA_INT, ADDRESS, JAVA_INT,
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    public static boolean ReadEventLog(MemorySegment hEventLog, int flags, MemorySegment buffer, int bufSize,
            MemorySegment bytesRead, MemorySegment minBytesNeeded) throws Throwable {
        return isSuccess(
                (int) ReadEventLog.invokeExact(hEventLog, flags, 0, buffer, bufSize, bytesRead, minBytesNeeded));
    }
}
