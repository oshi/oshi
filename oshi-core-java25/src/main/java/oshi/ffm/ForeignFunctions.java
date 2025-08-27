/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;

import java.lang.foreign.Arena;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;

public abstract class ForeignFunctions {

    protected static final Linker LINKER = Linker.nativeLinker();
    protected static final SymbolLookup SYMBOL_LOOKUP = SymbolLookup.loaderLookup();

    protected ForeignFunctions() {
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
}
