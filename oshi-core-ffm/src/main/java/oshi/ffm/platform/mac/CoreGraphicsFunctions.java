/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

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

    // uint32_t CGDisplayVendorNumber(CGDirectDisplayID display);

    private static final MethodHandle CGDisplayVendorNumber = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayVendorNumber"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    public static int CGDisplayVendorNumber(int display) throws Throwable {
        return (int) CGDisplayVendorNumber.invokeExact(display);
    }

    // CGRect CGDisplayBounds(CGDirectDisplayID display); — returns a struct { CGPoint origin; CGSize size; }, four
    // doubles: origin.x, origin.y, size.width and size.height at offsets 0, 8, 16 and 24

    private static final MemoryLayout CG_RECT_LAYOUT = MemoryLayout.structLayout(
            MemoryLayout.structLayout(JAVA_DOUBLE.withName("x"), JAVA_DOUBLE.withName("y")).withName("origin"),
            CG_SIZE_LAYOUT.withName("size"));

    private static final MethodHandle CGDisplayBounds = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayBounds"), FunctionDescriptor.of(CG_RECT_LAYOUT, JAVA_INT));

    public static MemorySegment CGDisplayBounds(SegmentAllocator allocator, int display) throws Throwable {
        return (MemorySegment) CGDisplayBounds.invokeExact(allocator, display);
    }

    // double CGDisplayRotation(CGDirectDisplayID display);

    private static final MethodHandle CGDisplayRotation = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayRotation"), FunctionDescriptor.of(JAVA_DOUBLE, JAVA_INT));

    public static double CGDisplayRotation(int display) throws Throwable {
        return (double) CGDisplayRotation.invokeExact(display);
    }

    // CGDisplayModeRef CGDisplayCopyDisplayMode(CGDirectDisplayID display);

    private static final MethodHandle CGDisplayCopyDisplayMode = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayCopyDisplayMode"), FunctionDescriptor.of(ADDRESS, JAVA_INT));

    public static MemorySegment CGDisplayCopyDisplayMode(int display) throws Throwable {
        return (MemorySegment) CGDisplayCopyDisplayMode.invokeExact(display);
    }

    // size_t CGDisplayModeGetPixelWidth(CGDisplayModeRef mode); — size_t is 64 bits on every macOS architecture

    private static final MethodHandle CGDisplayModeGetPixelWidth = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayModeGetPixelWidth"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CGDisplayModeGetPixelWidth(MemorySegment mode) throws Throwable {
        return (long) CGDisplayModeGetPixelWidth.invokeExact(mode);
    }

    // size_t CGDisplayModeGetPixelHeight(CGDisplayModeRef mode);

    private static final MethodHandle CGDisplayModeGetPixelHeight = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayModeGetPixelHeight"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CGDisplayModeGetPixelHeight(MemorySegment mode) throws Throwable {
        return (long) CGDisplayModeGetPixelHeight.invokeExact(mode);
    }

    // double CGDisplayModeGetRefreshRate(CGDisplayModeRef mode);

    private static final MethodHandle CGDisplayModeGetRefreshRate = LINKER.downcallHandle(
            CORE_GRAPHICS.findOrThrow("CGDisplayModeGetRefreshRate"), FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS));

    public static double CGDisplayModeGetRefreshRate(MemorySegment mode) throws Throwable {
        return (double) CGDisplayModeGetRefreshRate.invokeExact(mode);
    }

    // void CGDisplayModeRelease(CGDisplayModeRef mode);

    private static final MethodHandle CGDisplayModeRelease = LINKER
            .downcallHandle(CORE_GRAPHICS.findOrThrow("CGDisplayModeRelease"), FunctionDescriptor.ofVoid(ADDRESS));

    public static void CGDisplayModeRelease(MemorySegment mode) throws Throwable {
        CGDisplayModeRelease.invokeExact(mode);
    }

    // CGDirectDisplayID CGMainDisplayID();

    private static final MethodHandle CGMainDisplayID = LINKER
            .downcallHandle(CORE_GRAPHICS.findOrThrow("CGMainDisplayID"), FunctionDescriptor.of(JAVA_INT));

    public static int CGMainDisplayID() throws Throwable {
        return (int) CGMainDisplayID.invokeExact();
    }
}
