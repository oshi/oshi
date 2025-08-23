/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/**
 * Implementations of MacOS functions
 */
public class MacSystemImpl {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    private static final FunctionDescriptor PROC_LISTPIDS_FUNC = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT,
            ADDRESS, JAVA_INT);

    private static final MethodHandle proc_listpids = LINKER
            .downcallHandle(SYMBOL_LOOKUP.find("proc_listpids").orElseThrow(), PROC_LISTPIDS_FUNC);

    public static final int proc_listpids(int type, int typeinfo, MemorySegment pids, int bufferSize) throws Throwable {
        return (int) proc_listpids.invokeExact(type, typeinfo, pids, bufferSize);
    }
}
