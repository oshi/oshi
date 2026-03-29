/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for the CoreGraphics framework.
 * <p>
 * CoreGraphics provides low-level 2D rendering and, on macOS, services for working with display hardware and the
 * windowing system.
 */
public final class CoreGraphicsFunctions extends ForeignFunctions {

    private CoreGraphicsFunctions() {
    }

    private static final SymbolLookup CORE_GRAPHICS = lib("CoreGraphics.framework/CoreGraphics");

    // CFArrayRef CGWindowListCopyWindowInfo(CGWindowListOption option, CGWindowID relativeToWindow);

    private static final MethodHandle CGWindowListCopyWindowInfo = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGWindowListCopyWindowInfo"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT));

    public static MemorySegment CGWindowListCopyWindowInfo(int option, int relativeToWindow) throws Throwable {
        return (MemorySegment) CGWindowListCopyWindowInfo.invokeExact(option, relativeToWindow);
    }

    // bool CGRectMakeWithDictionaryRepresentation(CFDictionaryRef dict, CGRect *rect);

    private static final MethodHandle CGRectMakeWithDictionaryRepresentation = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGRectMakeWithDictionaryRepresentation"),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS));

    public static boolean CGRectMakeWithDictionaryRepresentation(MemorySegment dict, MemorySegment rect)
            throws Throwable {
        return (boolean) CGRectMakeWithDictionaryRepresentation.invokeExact(dict, rect);
    }
}
