/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

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

    private static final MethodHandle LookupPrivilegeValue = downcall(ADV, "LookupPrivilegeValueW", JAVA_INT, ADDRESS,
            ADDRESS, ADDRESS);

    public static boolean LookupPrivilegeValue(String name, MemorySegment luid, Arena arena) throws Throwable {
        MemorySegment nameSeg = toWideString(arena, name);
        return isSuccess((int) LookupPrivilegeValue.invokeExact(MemorySegment.NULL, nameSeg, luid));
    }

    private static final MethodHandle OpenProcessToken = downcall(ADV, "OpenProcessToken", JAVA_INT, ADDRESS, JAVA_INT,
            ADDRESS);

    public static boolean OpenProcessToken(MemorySegment process, int desiredAccess, MemorySegment hTokenOut)
            throws Throwable {
        return isSuccess((int) OpenProcessToken.invokeExact(process, desiredAccess, hTokenOut));
    }
}
