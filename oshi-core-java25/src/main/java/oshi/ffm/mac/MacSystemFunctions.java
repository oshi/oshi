/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Implementations of MacOS functions
 */
public class MacSystemFunctions {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    private static final MethodHandle proc_listpids = LINKER.downcallHandle(
            SYMBOL_LOOKUP.find("proc_listpids").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    public static final int proc_listpids(int type, int typeinfo, MemorySegment pids, int bufferSize) throws Throwable {
        return (int) proc_listpids.invokeExact(type, typeinfo, pids, bufferSize);
    }

    private static final MethodHandle proc_pidinfo = LINKER.downcallHandle(
            SYMBOL_LOOKUP.find("proc_pidinfo").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));

    public static int proc_pidinfo(int pid, int flavor, long arg, MemorySegment buffer, int bufferSize)
            throws Throwable {
        return (int) proc_pidinfo.invokeExact(pid, flavor, arg, buffer, bufferSize);
    }

    private static final MethodHandle getpid = LINKER.downcallHandle(SYMBOL_LOOKUP.find("getpid").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT));

    public static int getpid() throws Throwable {
        return (int) getpid.invokeExact();
    }

    private static final MethodHandle sysctlbyname = LINKER.downcallHandle(
            SYMBOL_LOOKUP.find("sysctlbyname").orElseThrow(),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, SIZE_T));

    public static int sysctlbyname(MemorySegment name, MemorySegment oldp, MemorySegment oldlenp, MemorySegment newp,
            long newlen) throws Throwable {
        return (int) sysctlbyname.invokeExact(name, oldp, newp, oldlenp, newlen);
    }
}
