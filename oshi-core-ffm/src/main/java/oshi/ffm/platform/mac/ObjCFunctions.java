/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.platform.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

/**
 * Objective-C runtime bindings for dispatching messages to AppKit classes via {@code objc_msgSend}. On ARM64,
 * {@code objc_msgSend} must be called with a typed signature matching the selector's actual parameter and return types.
 * Each method below matches a specific argument/return shape.
 */
public final class ObjCFunctions extends MacForeignFunctions {

    private ObjCFunctions() {
    }

    private static final SymbolLookup OBJC_LIBRARY = SymbolLookup.libraryLookup("libobjc.dylib", LIBRARY_ARENA);

    // id objc_getClass(const char * name);

    private static final MethodHandle objc_getClass = LINKER.downcallHandle(OBJC_LIBRARY.findOrThrow("objc_getClass"),
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment objc_getClass(MemorySegment name) throws Throwable {
        return (MemorySegment) objc_getClass.invokeExact(name);
    }

    // SEL sel_registerName(const char * str);

    private static final MethodHandle sel_registerName = LINKER
            .downcallHandle(OBJC_LIBRARY.findOrThrow("sel_registerName"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment sel_registerName(MemorySegment name) throws Throwable {
        return (MemorySegment) sel_registerName.invokeExact(name);
    }

    // id objc_msgSend(id self, SEL op);
    // id objc_msgSend(id self, SEL op, long arg) — for objectAtIndex: and similar

    private static final MethodHandle objc_msgSend = LINKER.downcallHandle(OBJC_LIBRARY.findOrThrow("objc_msgSend"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle objc_msgSend_long = LINKER.downcallHandle(
            OBJC_LIBRARY.findOrThrow("objc_msgSend"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));

    public static MemorySegment objc_msgSend(MemorySegment receiver, MemorySegment selector) throws Throwable {
        return (MemorySegment) objc_msgSend.invokeExact(receiver, selector);
    }

    public static MemorySegment objc_msgSend(MemorySegment receiver, MemorySegment selector, long arg)
            throws Throwable {
        return (MemorySegment) objc_msgSend_long.invokeExact(receiver, selector, arg);
    }

    // long objc_msgSend(id self, SEL op) — for count and similar returning NSUInteger

    private static final MethodHandle objc_msgSend_ret_long = LINKER.downcallHandle(
            OBJC_LIBRARY.findOrThrow("objc_msgSend"), FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS));

    public static long objc_msgSend_long(MemorySegment receiver, MemorySegment selector) throws Throwable {
        return (long) objc_msgSend_ret_long.invokeExact(receiver, selector);
    }

    // void * objc_autoreleasePoolPush(void);

    private static final MethodHandle objc_autoreleasePoolPush = LINKER
            .downcallHandle(OBJC_LIBRARY.findOrThrow("objc_autoreleasePoolPush"), FunctionDescriptor.of(ADDRESS));

    public static MemorySegment objc_autoreleasePoolPush() throws Throwable {
        return (MemorySegment) objc_autoreleasePoolPush.invokeExact();
    }

    // void objc_autoreleasePoolPop(void * context);

    private static final MethodHandle objc_autoreleasePoolPop = LINKER
            .downcallHandle(OBJC_LIBRARY.findOrThrow("objc_autoreleasePoolPop"), FunctionDescriptor.ofVoid(ADDRESS));

    public static void objc_autoreleasePoolPop(MemorySegment pool) throws Throwable {
        objc_autoreleasePoolPop.invokeExact(pool);
    }
}
