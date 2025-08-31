/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import oshi.ffm.ForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.WinNTFFM.OFFSET_ATTRIBUTES;
import static oshi.ffm.windows.WinNTFFM.OFFSET_LUID;
import static oshi.ffm.windows.WinNTFFM.OFFSET_PRIVILEGE_COUNT;
import static oshi.ffm.windows.WinNTFFM.SE_PRIVILEGE_ENABLED;
import static oshi.ffm.windows.WinNTFFM.TOKEN_PRIVILEGES;

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

    /**
     * Checks if the current OS is Windows Vista or greater.
     */
    public static boolean isVistaOrGreater() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");

        if (!osName.startsWith("Windows")) {
            return false;
        }

        String[] parts = osVersion.split("\\.");
        if (parts.length >= 2) {
            int major = Integer.parseInt(parts[0]);
            return major >= 6;
        }
        return false;
    }

    /**
     * Builds a TOKEN_PRIVILEGES struct with a single privilege enabled.
     *
     */
    public static MemorySegment setupTokenPrivileges(Arena arena, MemorySegment luid) {
        MemorySegment tkp = arena.allocate(TOKEN_PRIVILEGES);

        tkp.set(JAVA_INT, OFFSET_PRIVILEGE_COUNT, 1);

        MemorySegment luidSegment = tkp.asSlice(OFFSET_LUID, luid.byteSize());
        luidSegment.copyFrom(luid);

        tkp.set(JAVA_INT, OFFSET_ATTRIBUTES, SE_PRIVILEGE_ENABLED);

        return tkp;
    }

    /**
     * Allocate a null-terminated UTF-16 string in the given arena.
     */
    public static MemorySegment utf16(Arena arena, String s) {
        return arena.allocateFrom(s + "\0", StandardCharsets.UTF_16LE);
    }
}
