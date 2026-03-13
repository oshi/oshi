/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.WindowsForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM utilities for handling BSTR (Basic String) - the string type used in COM automation.
 * <p>
 * A BSTR is a pointer to a null-terminated Unicode string preceded by a 4-byte length prefix.
 * The pointer points to the first character, not the length prefix.
 * </p>
 */
public final class BStrFFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(BStrFFM.class);

    private static final SymbolLookup OLEAUT32 = lib("OleAut32.dll");

    private BStrFFM() {
    }

    // SysAllocString
    private static final MethodHandle SysAllocString = downcall(OLEAUT32, "SysAllocString", ADDRESS, ADDRESS);

    /**
     * Allocates a new BSTR and copies the specified string into it.
     *
     * @param arena the arena for string conversion
     * @param str   the string to convert
     * @return the BSTR pointer, or NULL if allocation failed
     */
    public static MemorySegment fromString(Arena arena, String str) {
        try {
            MemorySegment wideStr = toWideString(arena, str);
            return (MemorySegment) SysAllocString.invokeExact(wideStr);
        } catch (Throwable t) {
            LOG.debug("BStrFFM.fromString failed", t);
            return MemorySegment.NULL;
        }
    }

    // SysFreeString
    private static final MethodHandle SysFreeString = downcall(OLEAUT32, "SysFreeString", null, ADDRESS);

    /**
     * Frees a BSTR allocated by SysAllocString.
     *
     * @param bstr the BSTR to free
     */
    public static void free(MemorySegment bstr) {
        if (bstr == null || bstr.equals(MemorySegment.NULL)) {
            return;
        }
        try {
            SysFreeString.invokeExact(bstr);
        } catch (Throwable t) {
            LOG.debug("BStrFFM.free failed", t);
        }
    }

    // SysStringLen
    private static final MethodHandle SysStringLen = downcall(OLEAUT32, "SysStringLen", JAVA_INT, ADDRESS);

    /**
     * Returns the length of a BSTR in characters (not bytes).
     *
     * @param bstr the BSTR
     * @return the length in characters, or 0 if null
     */
    public static int length(MemorySegment bstr) {
        if (bstr == null || bstr.equals(MemorySegment.NULL)) {
            return 0;
        }
        try {
            return (int) SysStringLen.invokeExact(bstr);
        } catch (Throwable t) {
            LOG.debug("BStrFFM.length failed", t);
            return 0;
        }
    }

    /**
     * Converts a BSTR to a Java String.
     *
     * @param bstr  the BSTR pointer
     * @param arena the arena for memory reinterpretation
     * @return the Java string, or empty string if null
     */
    public static String toString(MemorySegment bstr, Arena arena) {
        if (bstr == null || bstr.equals(MemorySegment.NULL)) {
            return "";
        }
        int len = length(bstr);
        if (len == 0) {
            return "";
        }
        // Reinterpret the BSTR memory to read the characters
        MemorySegment strSeg = bstr.reinterpret((long) len * 2 + 2, arena, null);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = strSeg.get(JAVA_CHAR, (long) i * 2);
            sb.append(c);
        }
        return sb.toString();
    }
}
