/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

public final class CoreFoundationFunctions extends ForeignFunctions {

    public static final long ARRAY_TYPE_ID;
    public static final long BOOLEAN_TYPE_ID;
    public static final long DATA_TYPE_ID;
    public static final long DATE_TYPE_ID;
    public static final long DICTIONARY_TYPE_ID;
    public static final long NUMBER_TYPE_ID;
    public static final long STRING_TYPE_ID;
    static {
        try {
            ARRAY_TYPE_ID = CFArrayGetTypeID();
            BOOLEAN_TYPE_ID = CFBooleanGetTypeID();
            DATA_TYPE_ID = CFDataGetTypeID();
            DATE_TYPE_ID = CFDateGetTypeID();
            DICTIONARY_TYPE_ID = CFDictionaryGetTypeID();
            NUMBER_TYPE_ID = CFNumberGetTypeID();
            STRING_TYPE_ID = CFStringGetTypeID();
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

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

    // CFTypeRef CFRetain(CFTypeRef cf);

    private static final MethodHandle CFRetain = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFRetain"),
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment CFRetain(MemorySegment cf) throws Throwable {
        return (MemorySegment) CFRetain.invokeExact(cf);
    }

    // CFIndex CFGetRetainCount(CFTypeRef cf);

    private static final MethodHandle CFGetRetainCount = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFGetRetainCount"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFGetRetainCount(MemorySegment cf) throws Throwable {
        return (long) CFGetRetainCount.invokeExact(cf);
    }

    // Boolean CFEqual(CFTypeRef cf1, CFTypeRef cf2);

    private static final MethodHandle CFEqual = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFEqual"),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS));

    public static boolean CFEqual(MemorySegment cf1, MemorySegment cf2) throws Throwable {
        return (boolean) CFEqual.invokeExact(cf1, cf2);
    }

    // CFStringRef CFCopyDescription(CFTypeRef cf);

    private static final MethodHandle CFCopyDescription = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFCopyDescription"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment CFCopyDescription(MemorySegment cf) throws Throwable {
        return (MemorySegment) CFCopyDescription.invokeExact(cf);
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

    // CFIndex CFStringGetMaximumSizeForEncoding(CFIndex length, CFStringEncoding encoding);

    private static final MethodHandle CFStringGetMaximumSizeForEncoding = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFStringGetMaximumSizeForEncoding"),
            FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));

    public static long CFStringGetMaximumSizeForEncoding(long length, int encoding) throws Throwable {
        return (long) CFStringGetMaximumSizeForEncoding.invokeExact(length, encoding);
    }

    // Boolean CFNumberGetValue(CFNumberRef number, CFNumberType theType, void * valuePtr);

    private static final MethodHandle CFNumberGetValue = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFNumberGetValue"),
            FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, JAVA_INT, ADDRESS));

    public static boolean CFNumberGetValue(MemorySegment number, int theType, MemorySegment valuePtr) throws Throwable {
        return (boolean) CFNumberGetValue.invokeExact(number, theType, valuePtr);
    }

    // CFNumberType CFNumberGetType(CFNumberRef number);

    private static final MethodHandle CFNumberGetType = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFNumberGetType"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFNumberGetType(MemorySegment number) throws Throwable {
        return (long) CFNumberGetType.invokeExact(number);
    }

    // CFNumberRef CFNumberCreate(CFAllocatorRef allocator, CFNumberType theType, const void * valuePtr);

    private static final MethodHandle CFNumberCreate = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFNumberCreate"), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));

    public static MemorySegment CFNumberCreate(MemorySegment allocator, long theType, MemorySegment valuePtr)
            throws Throwable {
        return (MemorySegment) CFNumberCreate.invokeExact(allocator, theType, valuePtr);
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

    // CFDataRef CFDataCreate(CFAllocatorRef allocator, const UInt8 * bytes, CFIndex length);

    private static final MethodHandle CFDataCreate = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFDataCreate"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));

    public static MemorySegment CFDataCreate(MemorySegment allocator, MemorySegment bytes, long length)
            throws Throwable {
        return (MemorySegment) CFDataCreate.invokeExact(allocator, bytes, length);
    }

    // Boolean CFBooleanGetValue(CFBooleanRef boolean);

    private static final MethodHandle CFBooleanGetValue = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFBooleanGetValue"), FunctionDescriptor.of(JAVA_BYTE, ADDRESS));

    public static byte CFBooleanGetValue(MemorySegment bool) throws Throwable {
        return (byte) CFBooleanGetValue.invokeExact(bool);
    }

    // CFIndex CFArrayGetCount(CFArrayRef theArray);

    private static final MethodHandle CFArrayGetCount = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFArrayGetCount"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFArrayGetCount(MemorySegment theArray) throws Throwable {
        return (long) CFArrayGetCount.invokeExact(theArray);
    }

    // const void * CFArrayGetValueAtIndex(CFArrayRef theArray, CFIndex idx);

    private static final MethodHandle CFArrayGetValueAtIndex = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFArrayGetValueAtIndex"), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG));

    public static MemorySegment CFArrayGetValueAtIndex(MemorySegment theArray, long idx) throws Throwable {
        return (MemorySegment) CFArrayGetValueAtIndex.invokeExact(theArray, idx);
    }

    // CFArrayRef CFArrayCreate(CFAllocatorRef allocator, const void * * values, CFIndex numValues, const
    // CFArrayCallBacks * callBacks);

    private static final MethodHandle CFArrayCreate = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFArrayCreate"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));

    public static MemorySegment CFArrayCreate(MemorySegment allocator, MemorySegment values, long numValues,
            MemorySegment callbacks) throws Throwable {
        return (MemorySegment) CFArrayCreate.invokeExact(allocator, values, numValues, callbacks);
    }

    // CFIndex CFDictionaryGetCount(CFDictionaryRef theDict);

    private static final MethodHandle CFDictionaryGetCount = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDictionaryGetCount"), FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFDictionaryGetCount(MemorySegment theDict) throws Throwable {
        return (long) CFDictionaryGetCount.invokeExact(theDict);
    }

    // CFIndex CFDictionaryGetCount(CFDictionaryRef theDict);

    private static final MethodHandle CFDictionaryGetValue = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDictionaryGetValue"), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment CFDictionaryGetValue(MemorySegment theDict, MemorySegment key) throws Throwable {
        return (MemorySegment) CFDictionaryGetValue.invokeExact(theDict, key);
    }

    // CFIndex CFDictionaryGetCount(CFDictionaryRef theDict);

    private static final MethodHandle CFDictionaryGetValueIfPresent = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDictionaryGetValueIfPresent"),
            FunctionDescriptor.of(JAVA_BYTE, ADDRESS, ADDRESS, ADDRESS));

    public static byte CFDictionaryGetValueIfPresent(MemorySegment theDict, MemorySegment key, MemorySegment value)
            throws Throwable {
        return (byte) CFDictionaryGetValueIfPresent.invokeExact(theDict, key, value);
    }

    // void CFDictionarySetValue(CFMutableDictionaryRef theDict, const void * key, const void * value);

    private static final MethodHandle CFDictionarySetValue = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDictionarySetValue"), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));

    public static void CFDictionarySetValue(MemorySegment theDict, MemorySegment key, MemorySegment value)
            throws Throwable {
        CFDictionarySetValue.invokeExact(theDict, key, value);
    }

    // CFMutableDictionaryRef CFDictionaryCreateMutable(CFAllocatorRef allocator, CFIndex capacity, const
    // CFDictionaryKeyCallBacks *
    // keyCallBacks, const CFDictionaryValueCallBacks * valueCallBacks);

    private static final MethodHandle CFDictionaryCreateMutable = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDictionaryCreateMutable"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS));

    public static MemorySegment CFDictionaryCreateMutable(MemorySegment allocator, long capacity,
            MemorySegment keyCallBacks, MemorySegment valueCallBacks) throws Throwable {
        return (MemorySegment) CFDictionaryCreateMutable.invokeExact(allocator, capacity, keyCallBacks, valueCallBacks);
    }

    // CFMutableDictionaryRef CFDictionaryCreateMutable(CFAllocatorRef allocator, CFIndex capacity, const
    // CFDictionaryKeyCallBacks *
    // keyCallBacks, const CFDictionaryValueCallBacks * valueCallBacks);

    private static final MethodHandle CFGetTypeID = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFGetTypeID"),
            FunctionDescriptor.of(JAVA_LONG, ADDRESS));

    public static long CFGetTypeID(MemorySegment cf) throws Throwable {
        return (long) CFGetTypeID.invokeExact(cf);
    }

    // CFTypeID CFArrayGetTypeID();

    private static final MethodHandle CFArrayGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFArrayGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFArrayGetTypeID() throws Throwable {
        return (long) CFArrayGetTypeID.invokeExact();
    }

    // CFTypeID CFBooleanGetTypeID();

    private static final MethodHandle CFBooleanGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFBooleanGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFBooleanGetTypeID() throws Throwable {
        return (long) CFBooleanGetTypeID.invokeExact();
    }

    // CFTypeID CFDataGetTypeID();

    private static final MethodHandle CFDataGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFDataGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFDataGetTypeID() throws Throwable {
        return (long) CFDataGetTypeID.invokeExact();
    }

    // CFTypeID CFDateGetTypeID();

    private static final MethodHandle CFDateGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFDateGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFDateGetTypeID() throws Throwable {
        return (long) CFDateGetTypeID.invokeExact();
    }

    // CFTypeID CFDictionaryGetTypeID();

    private static final MethodHandle CFDictionaryGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFDictionaryGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFDictionaryGetTypeID() throws Throwable {
        return (long) CFDictionaryGetTypeID.invokeExact();
    }

    // CFTypeID CFNumberGetTypeID();

    private static final MethodHandle CFNumberGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFNumberGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFNumberGetTypeID() throws Throwable {
        return (long) CFNumberGetTypeID.invokeExact();
    }

    // CFTypeID CFStringGetTypeID();

    private static final MethodHandle CFStringGetTypeID = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFStringGetTypeID"), FunctionDescriptor.of(JAVA_LONG));

    public static long CFStringGetTypeID() throws Throwable {
        return (long) CFStringGetTypeID.invokeExact();
    }

    // CFLocaleRef CFLocaleCopyCurrent();

    private static final MethodHandle CFLocaleCopyCurrent = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("CFLocaleCopyCurrent"), FunctionDescriptor.of(ADDRESS));

    public static MemorySegment CFLocaleCopyCurrent() throws Throwable {
        return (MemorySegment) CFLocaleCopyCurrent.invokeExact();
    }

    // CFDateFormatterRef CFDateFormatterCreate(CFAllocatorRef allocator, CFLocaleRef locale, CFDateFormatterStyle
    // dateStyle,
    // CFDateFormatterStyle timeStyle);

    private static final MethodHandle CFDateFormatterCreate = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDateFormatterCreate"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG, JAVA_LONG));

    public static MemorySegment CFDateFormatterCreate(MemorySegment allocator, MemorySegment locale, long dateStyle,
            long timeStyle) throws Throwable {
        return (MemorySegment) CFDateFormatterCreate.invokeExact(allocator, locale, dateStyle, timeStyle);
    }

    // CFStringRef CFDateFormatterGetFormat(CFDateFormatterRef formatter);

    private static final MethodHandle CFDateFormatterGetFormat = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("CFDateFormatterGetFormat"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment CFDateFormatterGetFormat(MemorySegment formatter) throws Throwable {
        return (MemorySegment) CFDateFormatterGetFormat.invokeExact(formatter);
    }
}
