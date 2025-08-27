/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

/**
 * FFM bindings for DiskArbitration framework functions
 */
public final class DiskArbitrationFunctions extends ForeignFunctions {

    private static final MethodHandle DASessionCreate = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("DASessionCreate"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * Creates a new session.
     *
     * @param allocator The allocator to use, or NULL for the default allocator
     * @return A reference to a new DASession
     */
    public static MemorySegment DASessionCreate(MemorySegment allocator) {
        try {
            return (MemorySegment) DASessionCreate.invokeExact(allocator);
        } catch (Throwable e) {
            return MemorySegment.NULL;
        }
    }

    private static final MethodHandle DADiskCreateFromBSDName = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("DADiskCreateFromBSDName"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /**
     * Creates a new disk object from a BSD device name.
     *
     * @param allocator The allocator to use, or NULL for the default allocator
     * @param session   The DASession in which to contact Disk Arbitration
     * @param bsdName   The BSD device name
     * @return A reference to a new DADisk
     */
    public static MemorySegment DADiskCreateFromBSDName(MemorySegment allocator, MemorySegment session,
            MemorySegment bsdName) {
        try {
            return (MemorySegment) DADiskCreateFromBSDName.invokeExact(allocator, session, bsdName);
        } catch (Throwable e) {
            return MemorySegment.NULL;
        }
    }

    private static final MethodHandle DADiskCreateFromIOMedia = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("DADiskCreateFromIOMedia"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS));

    /**
     * Creates a new disk object from an IOMedia object.
     *
     * @param allocator The allocator to use, or NULL for the default allocator
     * @param session   The DASession in which to contact Disk Arbitration
     * @param media     The IOMedia object
     * @return A reference to a new DADisk
     */
    public static MemorySegment DADiskCreateFromIOMedia(MemorySegment allocator, MemorySegment session,
            MemorySegment media) {
        try {
            return (MemorySegment) DADiskCreateFromIOMedia.invokeExact(allocator, session, media);
        } catch (Throwable e) {
            return MemorySegment.NULL;
        }
    }

    private static final MethodHandle DADiskCopyDescription = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("DADiskCopyDescription"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * Obtains the Disk Arbitration description of the specified disk.
     *
     * @param disk The DADisk for which to obtain the description
     * @return The disk's Disk Arbitration description
     */
    public static MemorySegment DADiskCopyDescription(MemorySegment disk) {
        try {
            return (MemorySegment) DADiskCopyDescription.invokeExact(disk);
        } catch (Throwable e) {
            return MemorySegment.NULL;
        }
    }

    private static final MethodHandle DADiskGetBSDName = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("DADiskGetBSDName"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    /**
     * Obtains the BSD device name for the specified disk.
     *
     * @param disk The DADisk for which to obtain the BSD device name
     * @return The disk's BSD device name as a C string pointer
     */
    public static MemorySegment DADiskGetBSDName(MemorySegment disk) {
        try {
            return (MemorySegment) DADiskGetBSDName.invokeExact(disk);
        } catch (Throwable e) {
            return MemorySegment.NULL;
        }
    }
}
