/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

public final class CoreFoundationFunctions extends ForeignFunctions {

    // CFAllocatorRef CFAllocatorGetDefault();

    private static final MethodHandle CFAllocatorGetDefault = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFAllocatorGetDefault"), FunctionDescriptor.of(ADDRESS));

    public static MemorySegment CFAllocatorGetDefault() throws Throwable {
        return (MemorySegment) CFAllocatorGetDefault.invokeExact();
    }

    // void CFRelease(CFTypeRef cf);

    private static final MethodHandle CFRelease = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFRelease"),
            FunctionDescriptor.ofVoid(ADDRESS));

    public static void CFRelease(MemorySegment cf) throws Throwable {
        CFRelease.invokeExact(cf);
    }

    // CFStringRef CFStringCreateWithCharacters(CFAllocatorRef alloc, const UniChar * chars, CFIndex numChars);

    private static final MethodHandle CFStringCreateWithCharacters = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFStringCreateWithCharacters"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));

    public static MemorySegment CFStringCreateWithCharacters(MemorySegment allocator, MemorySegment chars,
            long numChars) throws Throwable {
        return (MemorySegment) CFStringCreateWithCharacters.invokeExact(allocator, chars, numChars);
    }

    // CFIndex CFStringGetLength(CFStringRef theString);

    private static final MethodHandle CFStringGetLength = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFStringGetLength"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFStringGetLength(MemorySegment theString) throws Throwable {
        return (long) CFStringGetLength.invokeExact(theString);
    }

    // Boolean CFStringGetCString(CFStringRef theString, char * buffer, CFIndex bufferSize, CFStringEncoding encoding);

    private static final MethodHandle CFStringGetCString = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFStringGetCString"),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT));

    public static boolean CFStringGetCString(MemorySegment theString, MemorySegment buffer, long bufferSize,
            int encoding) throws Throwable {
        return (boolean) CFStringGetCString.invokeExact(theString, buffer, bufferSize, encoding);
    }

    // Boolean CFNumberGetValue(CFNumberRef number, CFNumberType theType, void * valuePtr);

    private static final MethodHandle CFNumberGetValue = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFNumberGetValue"),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, JAVA_INT, ADDRESS));

    public static boolean CFNumberGetValue(MemorySegment number, int theType, MemorySegment valuePtr) throws Throwable {
        return (boolean) CFNumberGetValue.invokeExact(number, theType, valuePtr);
    }

    // CFIndex CFDataGetLength(CFDataRef theData);

    private static final MethodHandle CFDataGetLength = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFDataGetLength"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFDataGetLength(MemorySegment dataRef) throws Throwable {
        return (long) CFDataGetLength.invokeExact(dataRef);
    }

    // const UInt8 * CFDataGetBytePtr(CFDataRef theData);

    private static final MethodHandle CFDataGetBytePtr = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFDataGetBytePtr"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment CFDataGetBytePtr(MemorySegment dataRef) throws Throwable {
        return (MemorySegment) CFDataGetBytePtr.invokeExact(dataRef);
    }
}
