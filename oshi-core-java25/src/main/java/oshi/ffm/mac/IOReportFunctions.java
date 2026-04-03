/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * Bindings for the private IOReport framework (Apple Silicon GPU residency and energy counters).
 *
 * <p>
 * IOReport is a private Apple framework. It may not be present on all systems. {@link #isAvailable()} must be checked
 * before calling any method; calling wrapper methods when {@code isAvailable()} returns {@code false} will result in a
 * {@link NullPointerException} since the underlying handles are not initialized.
 */
public final class IOReportFunctions extends ForeignFunctions {

    private IOReportFunctions() {
    }

    private static final SymbolLookup IO_REPORT_LIBRARY = tryLoad();
    private static final boolean AVAILABLE = IO_REPORT_LIBRARY != null;

    private static SymbolLookup tryLoad() {
        // libIOReport.dylib lives in the dyld shared cache on macOS — not present as a file on
        // disk, but dlopen (used by SymbolLookup.libraryLookup) resolves it correctly, mirroring
        // JNA's Native.load("IOReport", ...) which maps to the same name via System.mapLibraryName.
        try {
            return SymbolLookup.libraryLookup(System.mapLibraryName("IOReport"), LIBRARY_ARENA);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Returns {@code true} if the IOReport library was successfully loaded and all symbols resolved.
     *
     * @return whether IOReport is available on this system
     */
    public static boolean isAvailable() {
        return AVAILABLE;
    }

    private static MethodHandle require(String symbol, FunctionDescriptor fd) {
        if (!AVAILABLE) {
            return null;
        }
        return LINKER.downcallHandle(IO_REPORT_LIBRARY.findOrThrow(symbol), fd);
    }

    // CFDictionaryRef IOReportCopyChannelsInGroup(CFStringRef group, CFStringRef subgroup, long a, long b, long c);

    private static final MethodHandle IOReportCopyChannelsInGroup = require("IOReportCopyChannelsInGroup",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_LONG));

    public static MemorySegment IOReportCopyChannelsInGroup(MemorySegment group, MemorySegment subgroup, long a, long b,
            long c) throws Throwable {
        return (MemorySegment) IOReportCopyChannelsInGroup.invokeExact(group, subgroup, a, b, c);
    }

    // void IOReportMergeChannels(CFDictionaryRef a, CFDictionaryRef b, CFTypeRef null3);

    private static final MethodHandle IOReportMergeChannels = require("IOReportMergeChannels",
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    public static void IOReportMergeChannels(MemorySegment a, MemorySegment b, MemorySegment null3) throws Throwable {
        IOReportMergeChannels.invokeExact(a, b, null3);
    }

    // IOReportSubscriptionRef IOReportCreateSubscription(void* a, CFDictionaryRef channels,
    // CFDictionaryRef* subscribedChannels, long b, CFTypeRef c);

    private static final MethodHandle IOReportCreateSubscription = require("IOReportCreateSubscription",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));

    public static MemorySegment IOReportCreateSubscription(MemorySegment a, MemorySegment channels,
            MemorySegment subscribedChannelsOut, long b, MemorySegment c) throws Throwable {
        return (MemorySegment) IOReportCreateSubscription.invokeExact(a, channels, subscribedChannelsOut, b, c);
    }

    // CFDictionaryRef IOReportCreateSamples(IOReportSubscriptionRef subscription,
    // CFDictionaryRef subscribedChannels, CFTypeRef reserved);

    private static final MethodHandle IOReportCreateSamples = require("IOReportCreateSamples",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment IOReportCreateSamples(MemorySegment subscription, MemorySegment subscribedChannels,
            MemorySegment reserved) throws Throwable {
        return (MemorySegment) IOReportCreateSamples.invokeExact(subscription, subscribedChannels, reserved);
    }

    // CFDictionaryRef IOReportCreateSamplesDelta(CFDictionaryRef a, CFDictionaryRef b, CFTypeRef reserved);

    private static final MethodHandle IOReportCreateSamplesDelta = require("IOReportCreateSamplesDelta",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment IOReportCreateSamplesDelta(MemorySegment a, MemorySegment b, MemorySegment reserved)
            throws Throwable {
        return (MemorySegment) IOReportCreateSamplesDelta.invokeExact(a, b, reserved);
    }

    // long IOReportSimpleGetIntegerValue(CFDictionaryRef channel, int reserved);

    private static final MethodHandle IOReportSimpleGetIntegerValue = require("IOReportSimpleGetIntegerValue",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));

    public static long IOReportSimpleGetIntegerValue(MemorySegment channel, int reserved) throws Throwable {
        return (long) IOReportSimpleGetIntegerValue.invokeExact(channel, reserved);
    }

    // CFStringRef IOReportChannelGetGroup(CFDictionaryRef channel);

    private static final MethodHandle IOReportChannelGetGroup = require("IOReportChannelGetGroup",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOReportChannelGetGroup(MemorySegment channel) throws Throwable {
        return (MemorySegment) IOReportChannelGetGroup.invokeExact(channel);
    }

    // CFStringRef IOReportChannelGetSubGroup(CFDictionaryRef channel);

    private static final MethodHandle IOReportChannelGetSubGroup = require("IOReportChannelGetSubGroup",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOReportChannelGetSubGroup(MemorySegment channel) throws Throwable {
        return (MemorySegment) IOReportChannelGetSubGroup.invokeExact(channel);
    }

    // CFStringRef IOReportChannelGetChannelName(CFDictionaryRef channel);

    private static final MethodHandle IOReportChannelGetChannelName = require("IOReportChannelGetChannelName",
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOReportChannelGetChannelName(MemorySegment channel) throws Throwable {
        return (MemorySegment) IOReportChannelGetChannelName.invokeExact(channel);
    }

    // int IOReportStateGetCount(CFDictionaryRef channel);

    private static final MethodHandle IOReportStateGetCount = require("IOReportStateGetCount",
            FunctionDescriptor.of(JAVA_INT, ADDRESS));

    public static int IOReportStateGetCount(MemorySegment channel) throws Throwable {
        return (int) IOReportStateGetCount.invokeExact(channel);
    }

    // CFStringRef IOReportStateGetNameForIndex(CFDictionaryRef channel, int index);

    private static final MethodHandle IOReportStateGetNameForIndex = require("IOReportStateGetNameForIndex",
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));

    public static MemorySegment IOReportStateGetNameForIndex(MemorySegment channel, int index) throws Throwable {
        return (MemorySegment) IOReportStateGetNameForIndex.invokeExact(channel, index);
    }

    // long IOReportStateGetResidency(CFDictionaryRef channel, int index);

    private static final MethodHandle IOReportStateGetResidency = require("IOReportStateGetResidency",
            FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));

    public static long IOReportStateGetResidency(MemorySegment channel, int index) throws Throwable {
        return (long) IOReportStateGetResidency.invokeExact(channel, index);
    }
}
