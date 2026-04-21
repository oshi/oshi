/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FFM bindings for Shell32 functions.
 */
public final class Shell32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(Shell32FFM.class);

    private static final SymbolLookup SHELL32 = lib("Shell32");

    // LPWSTR* CommandLineToArgvW(LPCWSTR lpCmdLine, int* pNumArgs)
    private static final MethodHandle CommandLineToArgvW = downcall(SHELL32, "CommandLineToArgvW", ADDRESS, ADDRESS,
            ADDRESS);

    /**
     * Parses a Unicode command line string and returns an array of pointers to the command line arguments.
     *
     * @param commandLine The command line string to parse
     * @return List of command line arguments, or empty list if parsing fails
     */
    public static List<String> CommandLineToArgv(String commandLine) {
        if (commandLine == null || commandLine.isEmpty()) {
            return Collections.emptyList();
        }

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cmdLineSegment = toWideString(arena, commandLine);
            MemorySegment numArgsSegment = arena.allocate(JAVA_INT);

            MemorySegment argvPtr = (MemorySegment) CommandLineToArgvW.invokeExact(cmdLineSegment, numArgsSegment);

            if (argvPtr == null || argvPtr.address() == 0) {
                return Collections.emptyList();
            }

            try {
                int numArgs = numArgsSegment.get(JAVA_INT, 0);
                if (numArgs <= 0) {
                    return Collections.emptyList();
                }

                List<String> args = new ArrayList<>(numArgs);
                // Reinterpret the pointer array with proper size
                MemorySegment argvArray = argvPtr.reinterpret((long) numArgs * ADDRESS.byteSize());

                for (int i = 0; i < numArgs; i++) {
                    MemorySegment argPtr = argvArray.get(ADDRESS, (long) i * ADDRESS.byteSize());
                    if (argPtr != null && argPtr.address() != 0) {
                        // Calculate string length by finding null terminator
                        // Reinterpret with large size to scan for null
                        MemorySegment scanSegment = argPtr.reinterpret(32768); // Max scan size
                        int charCount = 0;
                        while (charCount < 16384 && scanSegment.get(JAVA_CHAR, (long) charCount * 2) != 0) {
                            charCount++;
                        }
                        // Reinterpret to exact size needed (include null terminator)
                        MemorySegment argSegment = argPtr.reinterpret((long) (charCount + 1) * 2);
                        args.add(readWideString(argSegment));
                    }
                }

                return args;
            } finally {
                // Free the memory allocated by CommandLineToArgvW
                Kernel32FFM.LocalFree(argvPtr);
            }
        } catch (Throwable t) {
            LOG.debug("Shell32FFM.CommandLineToArgv failed: {}", t.getMessage());
            return Collections.emptyList();
        }
    }
}
