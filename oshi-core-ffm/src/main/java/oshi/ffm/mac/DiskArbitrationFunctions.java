/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public final class DiskArbitrationFunctions extends MacForeignFunctions {

    private DiskArbitrationFunctions() {
    }

    private static final SymbolLookup DA_LIBRARY = frameworkLookup("DiskArbitration");

    // DASessionRef DASessionCreate(CFAllocatorRef allocator);

    private static final MethodHandle DASessionCreate = LINKER.downcallHandle(DA_LIBRARY.findOrThrow("DASessionCreate"),
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment DASessionCreate(MemorySegment allocator) throws Throwable {
        return (MemorySegment) DASessionCreate.invokeExact(allocator);
    }

    // DADiskRef DADiskCreateFromBSDName(CFAllocatorRef allocator, DASessionRef session, const char * name);

    private static final MethodHandle DADiskCreateFromBSDName = LINKER.downcallHandle(
            DA_LIBRARY.findOrThrow("DADiskCreateFromBSDName"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment DADiskCreateFromBSDName(MemorySegment allocator, MemorySegment session,
            MemorySegment bsdName) throws Throwable {
        return (MemorySegment) DADiskCreateFromBSDName.invokeExact(allocator, session, bsdName);
    }

    // DADiskRef DADiskCreateFromIOMedia(CFAllocatorRef allocator, DASessionRef session, io_service_t media);

    private static final MethodHandle DADiskCreateFromIOMedia = LINKER.downcallHandle(
            DA_LIBRARY.findOrThrow("DADiskCreateFromIOMedia"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment DADiskCreateFromIOMedia(MemorySegment allocator, MemorySegment session,
            MemorySegment media) throws Throwable {
        return (MemorySegment) DADiskCreateFromIOMedia.invokeExact(allocator, session, media);
    }

    // CFDictionaryRef DADiskCopyDescription(DADiskRef disk);

    private static final MethodHandle DADiskCopyDescription = LINKER
            .downcallHandle(DA_LIBRARY.findOrThrow("DADiskCopyDescription"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment DADiskCopyDescription(MemorySegment disk) throws Throwable {
        return (MemorySegment) DADiskCopyDescription.invokeExact(disk);
    }

    // const char * DADiskGetBSDName(DADiskRef disk);

    private static final MethodHandle DADiskGetBSDName = LINKER
            .downcallHandle(DA_LIBRARY.findOrThrow("DADiskGetBSDName"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment DADiskGetBSDName(MemorySegment disk) throws Throwable {
        return (MemorySegment) DADiskGetBSDName.invokeExact(disk);
    }
}
