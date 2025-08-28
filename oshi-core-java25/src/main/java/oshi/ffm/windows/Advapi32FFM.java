/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class Advapi32FFM {

    private static final SymbolLookup ADV = WinFFM.lib("Advapi32");

    private static final MethodHandle OpenProcessToken = WinFFM.downcall(ADV, "OpenProcessToken",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));

    private static final MethodHandle LookupPrivilegeValueW = WinFFM.downcall(ADV, "LookupPrivilegeValueW",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    private static final MethodHandle AdjustTokenPrivileges = WinFFM.downcall(ADV, "AdjustTokenPrivileges",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));

    public static boolean openProcessToken(MemorySegment process, int desiredAccess, MemorySegment hTokenOut)
            throws Throwable {
        int ok = (int) OpenProcessToken.invokeExact(process, desiredAccess, hTokenOut);
        return ok != 0;
    }

    public static boolean lookupPrivilegeValue(String name, MemorySegment luid, Arena arena) throws Throwable {
        MemorySegment nameSeg = WinFFM.utf16(arena, name);
        int ok = (int) LookupPrivilegeValueW.invokeExact(MemorySegment.NULL, nameSeg, luid);
        return ok != 0;
    }

    public static boolean adjustTokenPrivileges(MemorySegment hToken, MemorySegment tkp) throws Throwable {
        int ok = (int) AdjustTokenPrivileges.invokeExact(hToken, 0, tkp, 0, MemorySegment.NULL, MemorySegment.NULL);
        return ok != 0;
    }
}
