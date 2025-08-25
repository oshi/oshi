/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static oshi.ffm.mac.MacSystemStructs.RLIMIT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * Implementations of MacOS functions
 */
public final class MacSystemFunctions {

    private MacSystemFunctions() {
    }

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    public static final ValueLayout.OfLong SIZE_T = ValueLayout.JAVA_LONG;

    // int proc_listpids(uint32_t type, uint32_t typeinfo, void *buffer, int buffersize)
    private static final MethodHandle proc_listpids = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("proc_listpids"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT));

    public static int proc_listpids(int type, int typeinfo, MemorySegment pids, int bufferSize) throws Throwable {
        return (int) proc_listpids.invokeExact(type, typeinfo, pids, bufferSize);
    }

    // int proc_pidinfo(int pid, int flavor, uint64_t arg, void *buffer, int buffersize)
    private static final MethodHandle proc_pidinfo = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("proc_pidinfo"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT));

    public static int proc_pidinfo(int pid, int flavor, long arg, MemorySegment buffer, int bufferSize)
            throws Throwable {
        return (int) proc_pidinfo.invokeExact(pid, flavor, arg, buffer, bufferSize);
    }

    // pid_t getpid(void);

    private static final MethodHandle getpid = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("getpid"),
            FunctionDescriptor.of(JAVA_INT));

    public static int getpid() throws Throwable {
        return (int) getpid.invokeExact();
    }

    // int sysctlbyname(const char *name, void *oldp, size_t *oldlenp, void *newp, size_t newlen);

    private static final MethodHandle sysctlbyname = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("sysctlbyname"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, SIZE_T));

    public static int sysctlbyname(MemorySegment name, MemorySegment oldp, MemorySegment oldlenp, MemorySegment newp,
            long newlen) throws Throwable {
        return (int) sysctlbyname.invokeExact(name, oldp, newp, oldlenp, newlen);
    }

    // int getrlimit(int resource, struct rlimit *rlp);

    private static final MethodHandle getrlimit = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("getrlimit"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, RLIMIT));

    public static int getrlimit(int resource, MemorySegment rlp) throws Throwable {
        return (int) getrlimit.invokeExact(resource, rlp);
    }
}
