/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.JAVA_CHAR;

/**
 * Utility class for working with the Foreign Function & Memory API (Java 24+). Provides helpers for library lookup,
 * downcalls, and UTF-16 string conversion.
 */
public final class WinFFM {

    private static final Linker LINKER = Linker.nativeLinker();

    private WinFFM() {
    }

    /**
     * Look up a native DLL by name (e.g., "Kernel32", "Advapi32").
     */
    public static SymbolLookup lib(String name) {
        return SymbolLookup.libraryLookup(name, Arena.global());
    }

    /**
     * Create a downcall handle for a native function.
     */
    public static MethodHandle downcall(SymbolLookup lib, String symbol, FunctionDescriptor fd) {
        MemorySegment sym = lib.find(symbol).orElseThrow(() -> new UnsatisfiedLinkError("Missing symbol: " + symbol));
        return LINKER.downcallHandle(sym, fd);
    }

    /**
     * Allocate a null-terminated UTF-16 string in the given arena.
     */
    public static MemorySegment utf16(Arena arena, String s) {
        char[] chars = (s + "\0").toCharArray();
        MemorySegment seg = arena.allocate(JAVA_CHAR, chars.length);
        for (int i = 0; i < chars.length; i++) {
            seg.setAtIndex(JAVA_CHAR, i, chars[i]);
        }
        return seg;
    }

}
