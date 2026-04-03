/*
 * Copyright 2025-2026 The OSHI Project Contributors
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

/**
 * Base class providing utility methods for working with the Java Foreign Function and Memory (FFM) API.
 * <p>
 * Subclasses use these helpers to load native libraries and frameworks, create downcall handles, and read data from
 * native memory segments.
 */
public abstract class ForeignFunctions {

    /** The native linker for the current platform. */
    protected static final Linker LINKER = Linker.nativeLinker();

    /** A shared auto arena used for library symbol lookups. */
    protected static final Arena LIBRARY_ARENA = Arena.ofAuto();

    /** Symbol lookup for libraries already loaded into the current process. */
    protected static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    /** Not intended for instantiation. */
    protected ForeignFunctions() {
    }

    /**
     * Lookup a native library by simple name, mapping it to the platform-specific filename (e.g. {@code "c"} →
     * {@code "libc.so"} on Linux).
     *
     * @param libraryName the platform-independent library name
     * @return the symbol lookup for the library
     */
    public static SymbolLookup libraryLookup(String libraryName) {
        return SymbolLookup.libraryLookup(System.mapLibraryName(libraryName), LIBRARY_ARENA);
    }

    /**
     * Reinterpret a raw native pointer as a struct of the given layout, scoped to the provided arena.
     *
     * @param pointer the native pointer
     * @param layout  the struct layout
     * @param arena   the arena to scope the resulting segment to
     * @return a memory segment over the struct, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
    public static MemorySegment getStructFromNativePointer(MemorySegment pointer, StructLayout layout, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        return MemorySegment.ofAddress(pointer.address()).reinterpret(layout.byteSize(), arena, null);
    }

    /**
     * Read a null-terminated UTF-8 string from a raw native pointer.
     *
     * @param pointer the native pointer
     * @param arena   the arena to scope the reinterpreted segment to
     * @return the Java string, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
    public static String getStringFromNativePointer(MemorySegment pointer, Arena arena) {
        if (pointer == null || pointer.equals(MemorySegment.NULL)) {
            return null;
        }
        // More than enough space for 255 UTF characters
        return MemorySegment.ofAddress(pointer.address()).reinterpret(1024, arena, null).getString(0);
    }

    /**
     * Copy {@code length} bytes from a raw native pointer into a Java byte array.
     *
     * @param pointer the native pointer
     * @param length  the number of bytes to copy
     * @param arena   the arena to scope the reinterpreted segment to
     * @return the byte array, or {@code null} if the pointer is null or {@link MemorySegment#NULL}
     */
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

    /** Linker option to capture {@code errno} after a native call. */
    protected static final Linker.Option CAPTURE_CALL_STATE = Linker.Option.captureCallState("errno");

    /** Layout of the captured call state segment, containing {@code errno}. */
    public static final StructLayout CAPTURED_STATE_LAYOUT = Linker.Option.captureStateLayout();

    /** Handle to read the {@code errno} field from a captured call state segment. */
    protected static final VarHandle ERRNO_HANDLE = CAPTURED_STATE_LAYOUT.varHandle(PathElement.groupElement("errno"));

    /**
     * Read the {@code errno} value from a captured call state segment.
     *
     * @param callState the memory segment returned by a call made with {@link #CAPTURE_CALL_STATE}
     * @return the {@code errno} value
     */
    public static int getErrno(MemorySegment callState) {
        return (int) ERRNO_HANDLE.get(callState, 0);
    }
}
