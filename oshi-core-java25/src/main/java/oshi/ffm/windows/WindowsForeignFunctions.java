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
import static oshi.ffm.windows.WinErrorFFM.ERROR_SUCCESS;
import static oshi.ffm.windows.WinNTFFM.SE_PRIVILEGE_ENABLED;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES_ATTRIBUTES_OFFSET;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES_LUID_OFFSET;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES_PRIVILEGE_COUNT_OFFSET;

/**
 * Utility class for working with the Foreign Function and Memory API. Provides helpers for library lookup, downcalls,
 * and UTF-16 string conversion.
 */
public abstract class WindowsForeignFunctions extends ForeignFunctions {

    protected WindowsForeignFunctions() {
    }

    /**
     * Validates a Windows API return code, allowing {@code ERROR_SUCCESS} or specified codes.
     *
     * @param rc            return code from a Windows API call
     * @param allowedErrors optional additional codes
     * @return the original code if valid
     * @throws Win32Exception if not successful
     */
    public static int checkSuccess(int rc, int... allowedErrors) {
        if (rc == ERROR_SUCCESS) {
            return rc;
        }
        for (int allowed : allowedErrors) {
            if (rc == allowed) {
                return rc;
            }
        }
        throw new Win32Exception(rc);
    }

    /**
     * Converts a Windows BOOL (0 or non-zero) to a Java boolean. In Windows API, 0 means FALSE and non-zero means TRUE.
     *
     * @param winBool the BOOL value returned by a Windows API
     * @return {@code true} if non-zero, {@code false} if zero
     */
    public static boolean isSuccess(int winBool) {
        return winBool != 0;
    }

    /**
     * Reads a null-terminated UTF-16 wide string from the given memory segment.
     *
     * @param seg the memory segment containing the UTF-16 string
     * @return the decoded Java string
     */
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

    /**
     * Reads a null-terminated ANSI (single-byte) string from the given memory segment.
     *
     * @param seg    the memory segment containing the ANSI string
     * @param maxLen the maximum number of bytes to read
     * @return the decoded Java string
     */
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
     * Creates and initializes a {@code TOKEN_PRIVILEGES} structure with one privilege enabled.
     *
     * @param arena the memory arena used for allocation
     * @param luid  the LUID of the privilege to enable
     * @return a memory segment containing the initialized {@code TOKEN_PRIVILEGES} structure
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
     * Converts a Java string into a null-terminated UTF-16LE wide string memory segment.
     *
     * @param arena the memory arena used for allocation
     * @param s     the Java string to convert
     * @return a memory segment containing the UTF-16LE encoded string with a null terminator
     */
    public static MemorySegment toWideString(Arena arena, String s) {
        return arena.allocateFrom(s + "\0", StandardCharsets.UTF_16LE);
    }
}
