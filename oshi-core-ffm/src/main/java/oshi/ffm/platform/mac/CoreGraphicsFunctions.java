/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/**
 * FFM bindings for the CoreGraphics framework.
 * <p>
 * CoreGraphics provides low-level 2D rendering and, on macOS, services for working with display hardware and the
 * windowing system.
 */
public final class CoreGraphicsFunctions extends MacForeignFunctions {

    private CoreGraphicsFunctions() {
    }

    private static final SymbolLookup CORE_GRAPHICS = frameworkLookup("CoreGraphics");

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

    // CGError CGGetActiveDisplayList(uint32_t maxDisplays, CGDirectDisplayID *activeDisplays, uint32_t *displayCount);

    private static final MethodHandle CGGetActiveDisplayList = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGGetActiveDisplayList"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));

    public static int CGGetActiveDisplayList(int maxDisplays, MemorySegment activeDisplays, MemorySegment displayCount)
            throws Throwable {
        return (int) CGGetActiveDisplayList.invokeExact(maxDisplays, activeDisplays, displayCount);
    }

    // boolean CGDisplayIsBuiltin(CGDirectDisplayID display);

    private static final MethodHandle CGDisplayIsBuiltin = LINKER
            .downcallHandle(CORE_GRAPHICS.findOrThrow("CGDisplayIsBuiltin"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    public static int CGDisplayIsBuiltin(int display) throws Throwable {
        return (int) CGDisplayIsBuiltin.invokeExact(display);
    }

    // uint32_t CGDisplayModelNumber(CGDirectDisplayID display);

    private static final MethodHandle CGDisplayModelNumber = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayModelNumber"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    public static int CGDisplayModelNumber(int display) throws Throwable {
        return (int) CGDisplayModelNumber.invokeExact(display);
    }

    // uint32_t CGDisplaySerialNumber(CGDirectDisplayID display);

    private static final MethodHandle CGDisplaySerialNumber = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplaySerialNumber"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    public static int CGDisplaySerialNumber(int display) throws Throwable {
        return (int) CGDisplaySerialNumber.invokeExact(display);
    }

    // CGSize CGDisplayScreenSize(CGDirectDisplayID display); — returns a struct { double width; double height; }

    private static final MemoryLayout CG_SIZE_LAYOUT = MemoryLayout.structLayout(JAVA_DOUBLE.withName("width"),
            JAVA_DOUBLE.withName("height"));

    private static final MethodHandle CGDisplayScreenSize = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayScreenSize"), FunctionDescriptor.of(CG_SIZE_LAYOUT, JAVA_INT));

    public static MemorySegment CGDisplayScreenSize(SegmentAllocator allocator, int display) throws Throwable {
        return (MemorySegment) CGDisplayScreenSize.invokeExact(allocator, display);
    }
}
