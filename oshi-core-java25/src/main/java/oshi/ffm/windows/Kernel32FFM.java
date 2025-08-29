/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public final class Kernel32FFM {

    private static final SymbolLookup K32 = WinFFM.lib("Kernel32");

    private static final MethodHandle CloseHandle = WinFFM.downcall(K32, "CloseHandle",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    private static final MethodHandle GetCurrentProcess = WinFFM.downcall(K32, "GetCurrentProcess",
            FunctionDescriptor.of(ADDRESS));

    private static final MethodHandle GetCurrentProcessId = WinFFM.downcall(K32, "GetCurrentProcessId",
            FunctionDescriptor.of(JAVA_INT) // returns int, no args
    );

    private static final MethodHandle GetLastError = WinFFM.downcall(K32, "GetLastError",
            FunctionDescriptor.of(JAVA_INT));

    private static final MethodHandle GetTickCount64 = WinFFM.downcall(
        K32,
        "GetTickCount64",
        FunctionDescriptor.of(JAVA_LONG) // 64-bit return
    );

    private static final MethodHandle GetTickCount = WinFFM.downcall(
        K32,
        "GetTickCount",
        FunctionDescriptor.of(JAVA_INT) // 32-bit return
    );

    public static void closeHandle(MemorySegment handle) {
        try {
            CloseHandle.invokeExact(handle);
        } catch (Throwable t) {
            throw new RuntimeException("Kernel32FFM.closeHandle failed", t);
        }
    }

    public static MemorySegment getCurrentProcess() throws Throwable {
        try {
            return (MemorySegment) GetCurrentProcess.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Kernel32FFM.getCurrentProcess failed", t);
        }
    }

    public static int getCurrentProcessId() {
        try {
            return (int) GetCurrentProcessId.invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException("Kernel32FFM.getCurrentProcessId failed", t);
        }
    }

    public static int getLastError() {
        try {
            return (int) GetLastError.invokeExact();
        } catch (Throwable t) {
            return -1;
        }
    }

    public static long querySystemUptime(boolean isVistaOrGreater) {
        try {
            if (isVistaOrGreater) {
                return ((long) GetTickCount64.invokeExact()) / 1000L;
            } else {
                return ((int) GetTickCount.invokeExact()) / 1000L;
            }
        } catch (Throwable t) {
            throw new RuntimeException("Kernel32FFM.querySystemUptime failed", t);
        }
    }
}
