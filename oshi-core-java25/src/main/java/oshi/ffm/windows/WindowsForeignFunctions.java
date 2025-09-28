/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import oshi.ffm.ForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.WinNTFFM.SE_PRIVILEGE_ENABLED;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES_ATTRIBUTES_OFFSET;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES_LUID_OFFSET;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES_PRIVILEGE_COUNT_OFFSET;

/**
 * Utility class for working with the Foreign Function & Memory API (Java 24+). Provides helpers for library lookup,
 * downcalls, and UTF-16 string conversion.
 */
public abstract class WindowsForeignFunctions extends ForeignFunctions {

    protected WindowsForeignFunctions() {
    }

    /**
     * Converts Windows BOOL return value to Java boolean. In Windows APIs, 0 = FALSE (failure), non-zero = TRUE
     * (success)
     */
    public static boolean isSuccess(int winBool) {
        return winBool != 0;
    }

    public static String readWideString(MemorySegment seg) {
        StringBuilder sb = new StringBuilder();
        for (int offset = 0;; offset += 2) { // 2 bytes per UTF-16 char
            char c = seg.get(JAVA_CHAR, offset);
            if (c == '\0')
                break; // null terminator
            sb.append(c);
        }
        return sb.toString();
    }

    public static String readAnsiString(MemorySegment seg, int maxLen) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLen; i++) {
            byte b = seg.get(JAVA_BYTE, i);
            if (b == 0)
                break;
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * Builds a TOKEN_PRIVILEGES struct with a single privilege enabled.
     *
     */
    public static MemorySegment setupTokenPrivileges(Arena arena, MemorySegment luid) {
        MemorySegment tkp = arena.allocate(TOKEN_PRIVILEGES);

        tkp.set(JAVA_INT, TOKEN_PRIVILEGES_PRIVILEGE_COUNT_OFFSET, 1);

        MemorySegment luidSegment = tkp.asSlice(TOKEN_PRIVILEGES_LUID_OFFSET, luid.byteSize());
        luidSegment.copyFrom(luid);

        tkp.set(JAVA_INT, TOKEN_PRIVILEGES_ATTRIBUTES_OFFSET, SE_PRIVILEGE_ENABLED);

        return tkp;
    }

    /**
     * Allocate a null-terminated UTF-16 string in the given arena.
     */
    public static MemorySegment toWideString(Arena arena, String s) {
        return arena.allocateFrom(s + "\0", StandardCharsets.UTF_16LE);
    }
}
