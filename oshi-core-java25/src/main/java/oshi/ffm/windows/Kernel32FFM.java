package oshi.ffm.windows;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class Kernel32FFM {

    private static final SymbolLookup K32 = WinFFM.lib("Kernel32");

    private static final MethodHandle GetCurrentProcess = WinFFM.downcall(
        K32, "GetCurrentProcess", FunctionDescriptor.of(ADDRESS)
    );

    private static final MethodHandle CloseHandle = WinFFM.downcall(
        K32, "CloseHandle", FunctionDescriptor.of(JAVA_INT, ADDRESS)
    );

    public static MemorySegment getCurrentProcess() throws Throwable {
        return (MemorySegment) GetCurrentProcess.invokeExact();
    }

    public static void closeHandle(MemorySegment handle) {
        try { CloseHandle.invokeExact(handle); } catch (Throwable ignored) {}
    }
}
