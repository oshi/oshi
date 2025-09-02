/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public final class Kernel32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(Kernel32FFM.class);

    private static final SymbolLookup K32 = lib("Kernel32");

    private static final MethodHandle CloseHandle = downcall(K32, "CloseHandle", JAVA_INT, ADDRESS);

    public static OptionalInt CloseHandle(MemorySegment handle) {
        try {
            return OptionalInt.of((int) CloseHandle.invokeExact(handle));
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.closeHandle failed", t);
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetCurrentProcess = downcall(K32, "GetCurrentProcess", ADDRESS);

    public static Optional<MemorySegment> GetCurrentProcess() {
        try {
            return Optional.of((MemorySegment) GetCurrentProcess.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetCurrentProcess failed: {}", t.getMessage());
            return Optional.empty();
        }
    }

    private static final MethodHandle GetCurrentProcessId = downcall(K32, "GetCurrentProcessId", JAVA_INT);

    public static OptionalInt GetCurrentProcessId() {
        try {
            return OptionalInt.of((int) GetCurrentProcessId.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetCurrentProcessId failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetLastError = downcall(K32, "GetLastError", JAVA_INT);

    public static OptionalInt GetLastError() {
        try {
            return OptionalInt.of((int) GetLastError.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetLastError failed: {}", t.getMessage());
            return OptionalInt.empty();
        }
    }

    private static final MethodHandle GetTickCount = downcall(K32, "GetTickCount64", JAVA_LONG);

    public static OptionalLong GetTickCount() {
        try {
            return OptionalLong.of((long) GetTickCount.invokeExact());
        } catch (Throwable t) {
            LOG.debug("Kernel32FFM.GetTickCount64 failed: {}", t.getMessage());
            return OptionalLong.empty();
        }
    }
}
