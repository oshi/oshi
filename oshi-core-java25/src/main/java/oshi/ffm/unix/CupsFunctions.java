/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.unix;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for the CUPS (Common Unix Printing System) library.
 * <p>
 * CUPS is available as {@code libcups} on Linux and macOS (via the dyld shared cache as {@code libcups.dylib}).
 */
public final class CupsFunctions extends ForeignFunctions {

    private CupsFunctions() {
    }

    /** IPP printer state: idle. */
    public static final int IPP_PRINTER_IDLE = 3;
    /** IPP printer state: processing (printing). */
    public static final int IPP_PRINTER_PROCESSING = 4;
    /** IPP printer state: stopped. */
    public static final int IPP_PRINTER_STOPPED = 5;

    /** Printer type bit flag indicating a remote (network) printer. */
    public static final int CUPS_PRINTER_REMOTE = 0x0002;

    /**
     * Layout of {@code cups_dest_t}:
     *
     * <pre>
     * typedef struct {
     *     char          *name;        // Printer or class name
     *     char          *instance;    // Local instance name or NULL
     *     int           is_default;   // Is this printer the default?
     *     int           num_options;  // Number of options
     *     cups_option_t *options;     // Options
     * } cups_dest_t;
     * </pre>
     */
    public static final StructLayout CUPS_DEST_T = MemoryLayout.structLayout(ADDRESS.withName("name"),
            ADDRESS.withName("instance"), JAVA_INT.withName("is_default"), JAVA_INT.withName("num_options"),
            ADDRESS.withName("options"));

    /** VarHandle for the {@code name} field of {@code cups_dest_t}. */
    public static final VarHandle CUPS_DEST_NAME = CUPS_DEST_T.varHandle(MemoryLayout.PathElement.groupElement("name"));
    /** VarHandle for the {@code is_default} field of {@code cups_dest_t}. */
    public static final VarHandle CUPS_DEST_IS_DEFAULT = CUPS_DEST_T
            .varHandle(MemoryLayout.PathElement.groupElement("is_default"));
    /** VarHandle for the {@code num_options} field of {@code cups_dest_t}. */
    public static final VarHandle CUPS_DEST_NUM_OPTIONS = CUPS_DEST_T
            .varHandle(MemoryLayout.PathElement.groupElement("num_options"));
    /** VarHandle for the {@code options} field of {@code cups_dest_t}. */
    public static final VarHandle CUPS_DEST_OPTIONS = CUPS_DEST_T
            .varHandle(MemoryLayout.PathElement.groupElement("options"));

    private static final SymbolLookup CUPS_LIBRARY;
    private static final boolean AVAILABLE;

    // int cupsGetDests(cups_dest_t **dests);
    private static final MethodHandle cupsGetDests;
    // void cupsFreeDests(int num_dests, cups_dest_t *dests);
    private static final MethodHandle cupsFreeDests;
    // const char *cupsGetOption(const char *name, int num_options, cups_option_t *options);
    private static final MethodHandle cupsGetOption;
    // const char *cupsGetDefault(void);
    private static final MethodHandle cupsGetDefault;

    static {
        SymbolLookup lookup = null;
        boolean available = false;
        MethodHandle hGetDests = null;
        MethodHandle hFreeDests = null;
        MethodHandle hGetOption = null;
        MethodHandle hGetDefault = null;
        try {
            lookup = libraryLookup("cups");
            hGetDests = LINKER.downcallHandle(lookup.findOrThrow("cupsGetDests"),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS));
            hFreeDests = LINKER.downcallHandle(lookup.findOrThrow("cupsFreeDests"),
                    FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS));
            hGetOption = LINKER.downcallHandle(lookup.findOrThrow("cupsGetOption"),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
            hGetDefault = LINKER.downcallHandle(lookup.findOrThrow("cupsGetDefault"), FunctionDescriptor.of(ADDRESS));
            available = true;
        } catch (Throwable e) {
            // libcups not available or symbol binding failed; callers should fall back to lpstat
        }
        CUPS_LIBRARY = lookup;
        AVAILABLE = available;
        cupsGetDests = hGetDests;
        cupsFreeDests = hFreeDests;
        cupsGetOption = hGetOption;
        cupsGetDefault = hGetDefault;
    }

    /**
     * Returns whether the CUPS library was successfully loaded and all symbols bound.
     *
     * @return {@code true} if libcups is available
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    /**
     * Gets all available CUPS destinations (printers and classes).
     *
     * @param dests a pointer-to-pointer that will be set to the allocated destination array
     * @return the number of destinations, or 0 on failure
     * @throws Throwable if the native call fails
     */
    public static int cupsGetDests(MemorySegment dests) throws Throwable {
        return (int) cupsGetDests.invokeExact(dests);
    }

    /**
     * Frees the memory allocated by {@link #cupsGetDests}.
     *
     * @param numDests the number of destinations returned by {@link #cupsGetDests}
     * @param dests    the destination array pointer
     * @throws Throwable if the native call fails
     */
    public static void cupsFreeDests(int numDests, MemorySegment dests) throws Throwable {
        cupsFreeDests.invokeExact(numDests, dests);
    }

    /**
     * Gets the value of a named option from a destination's options array.
     *
     * @param name       the option name as a native string segment
     * @param numOptions the number of options
     * @param options    pointer to the options array
     * @return a native segment pointing to the option value string, or a NULL segment if not found
     * @throws Throwable if the native call fails
     */
    public static MemorySegment cupsGetOption(MemorySegment name, int numOptions, MemorySegment options)
            throws Throwable {
        return (MemorySegment) cupsGetOption.invokeExact(name, numOptions, options);
    }

    /**
     * Gets the default printer name.
     *
     * @return a native segment pointing to the default printer name, or a NULL segment if none is set
     * @throws Throwable if the native call fails
     */
    public static MemorySegment cupsGetDefault() throws Throwable {
        return (MemorySegment) cupsGetDefault.invokeExact();
    }

    /**
     * Convenience method to read a named option value from a destination's options segment.
     *
     * @param optionName the option name
     * @param numOptions the number of options in the array
     * @param options    the native options pointer
     * @param arena      the arena to allocate the name string in
     * @return the option value, or an empty string if not found
     */
    public static String getOption(String optionName, int numOptions, MemorySegment options, Arena arena) {
        try {
            MemorySegment nameSegment = arena.allocateFrom(optionName);
            MemorySegment result = cupsGetOption(nameSegment, numOptions, options);
            if (result == null || result.equals(MemorySegment.NULL)) {
                return "";
            }
            return getStringFromNativePointer(result, arena);
        } catch (Throwable e) {
            return "";
        }
    }
}
