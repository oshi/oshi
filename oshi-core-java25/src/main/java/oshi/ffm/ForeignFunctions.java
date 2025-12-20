/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

public abstract class ForeignFunctions {

    protected static final Linker LINKER = Linker.nativeLinker();
    protected static final Arena LIBRARY_ARENA = Arena.ofAuto();
    protected static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    protected ForeignFunctions() {
    }

    public static SymbolLookup libraryLookup(String libraryName) {
        return SymbolLookup.libraryLookup(System.mapLibraryName(libraryName), LIBRARY_ARENA);
    }

    public static MemorySegment getStructFromNativePointer(MemorySegment pointer, StructLayout layout, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        return MemorySegment.ofAddress(pointer.address()).reinterpret(layout.byteSize(), arena, null);
    }

    public static String getStringFromNativePointer(MemorySegment pointer, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        // More than enough space for 255 UTF characters
        return MemorySegment.ofAddress(pointer.address()).reinterpret(1024, arena, null).getString(0);
    }

    public static byte[] getByteArrayFromNativePointer(MemorySegment pointer, long length, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        MemorySegment bytesSegment = MemorySegment.ofAddress(pointer.address()).reinterpret(length, arena, null);
        byte[] result = new byte[(int) length];
        MemorySegment.copy(bytesSegment, JAVA_BYTE, 0, result, 0, (int) length);
        return result;
    }

    /**
     * Lookup a library by name in the global arena.
     *
     * @param name the library name
     * @return the symbol lookup for the library
     */
    public static SymbolLookup lib(String name) {
        return SymbolLookup.libraryLookup(name, Arena.global());
    }

    /**
     * Create a downcall handle for a symbol in a library.
     *
     * @param lib        the symbol lookup
     * @param symbol     the symbol name
     * @param resLayout  the return layout
     * @param argLayouts the argument layouts
     * @return the method handle
     */
    public static MethodHandle downcall(SymbolLookup lib, String symbol, MemoryLayout resLayout,
            MemoryLayout... argLayouts) {
        MemorySegment sym = lib.findOrThrow(symbol);
        FunctionDescriptor fd = (resLayout == null) ? FunctionDescriptor.ofVoid(argLayouts)
                : FunctionDescriptor.of(resLayout, argLayouts);
        return LINKER.downcallHandle(sym, fd);
    }

    protected static final Linker.Option CAPTURE_CALL_STATE = Linker.Option.captureCallState("errno");
    public static final StructLayout CAPTURED_STATE_LAYOUT = Linker.Option.captureStateLayout();
    protected static final VarHandle ERRNO_HANDLE = CAPTURED_STATE_LAYOUT.varHandle(PathElement.groupElement("errno"));

    public static int getErrno(MemorySegment callState) {
        return (int) ERRNO_HANDLE.get(callState, 0);
    }
}
