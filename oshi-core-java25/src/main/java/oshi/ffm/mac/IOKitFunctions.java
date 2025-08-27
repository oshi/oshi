/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import oshi.ffm.ForeignFunctions;

public final class IOKitFunctions extends ForeignFunctions {

    private IOKitFunctions() {
    }

    /*
     * IOKitLib.h
     */

    // kern_return_t IOMasterPort(mach_port_t bootstrapPort, mach_port_t *mainPort);

    private static final MethodHandle IOMasterPort = LINKER.downcallHandle(SYMBOL_LOOKUP.findOrThrow("IOMasterPort"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    public static int IOMasterPort(int bootstrapPort, MemorySegment port) throws Throwable {
        return (int) IOMasterPort.invokeExact(bootstrapPort, port);
    }

    // io_registry_entry_t IORegistryGetRootEntry(mach_port_t mainPort);

    private static final MethodHandle IORegistryGetRootEntry = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryGetRootEntry"), FunctionDescriptor.of(ADDRESS, JAVA_INT));

    public static MemorySegment IORegistryGetRootEntry(int masterPort) throws Throwable {
        return (MemorySegment) IORegistryGetRootEntry.invokeExact(masterPort);
    }

    // CFMutableDictionaryRef IOServiceNameMatching(const char *name);

    private static final MethodHandle IOServiceNameMatching = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOServiceNameMatching"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOServiceNameMatching(MemorySegment name) throws Throwable {
        return (MemorySegment) IOServiceNameMatching.invokeExact(name);
    }

    // CFMutableDictionaryRef IOServiceMatching(const char *name);

    private static final MethodHandle IOServiceMatching = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("IOServiceMatching"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOServiceMatching(MemorySegment name) throws Throwable {
        return (MemorySegment) IOServiceMatching.invokeExact(name);
    }

    // io_service_t IOServiceGetMatchingService(mach_port_t mainPort, CFDictionaryRef matching);

    private static final MethodHandle IOServiceGetMatchingService = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOServiceGetMatchingService"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS));

    public static MemorySegment IOServiceGetMatchingService(int masterPort, MemorySegment matchingDict)
            throws Throwable {
        return (MemorySegment) IOServiceGetMatchingService.invokeExact(masterPort, matchingDict);
    }

    // kern_return_t IOServiceGetMatchingServices(mach_port_t mainPort, CFDictionaryRef matching, io_iterator_t
    // *existing);

    private static final MethodHandle IOServiceGetMatchingServices = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOServiceGetMatchingServices"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));

    public static int IOServiceGetMatchingServices(int masterPort, MemorySegment matchingDict, MemorySegment iterator)
            throws Throwable {
        return (int) IOServiceGetMatchingServices.invokeExact(masterPort, matchingDict, iterator);
    }

    // CFMutableDictionaryRef IOBSDNameMatching(mach_port_t mainPort, uint32_t options, const char *bsdName);

    private static final MethodHandle IOBSDNameMatching = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOBSDNameMatching"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    public static MemorySegment IOBSDNameMatching(int masterPort, int options, MemorySegment bsdName) throws Throwable {
        return (MemorySegment) IOBSDNameMatching.invokeExact(masterPort, options, bsdName);
    }

    // kern_return_t IOObjectRelease(io_object_t object);

    private static final MethodHandle IOObjectRelease = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("IOObjectRelease"), FunctionDescriptor.of(JAVA_INT, ADDRESS));

    public static int IOObjectRelease(MemorySegment object) throws Throwable {
        return (int) IOObjectRelease.invokeExact(object);
    }

    // boolean_t IOObjectConformsTo(io_object_t object, const io_name_t className);

    private static final MethodHandle IOObjectConformsTo = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOObjectConformsTo"), FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS));

    public static boolean IOObjectConformsTo(MemorySegment object, MemorySegment className) throws Throwable {
        return (boolean) IOObjectConformsTo.invokeExact(object, className);
    }

    // io_object_t IOIteratorNext(io_iterator_t iterator);

    private static final MethodHandle IOIteratorNext = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("IOIteratorNext"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOIteratorNext(MemorySegment iterator) throws Throwable {
        return (MemorySegment) IOIteratorNext.invokeExact(iterator);
    }

    // kern_return_t IORegistryEntryGetRegistryEntryID(io_registry_entry_t entry, uint64_t *entryID);

    private static final MethodHandle IORegistryEntryGetRegistryEntryID = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryGetRegistryEntryID"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetRegistryEntryID(MemorySegment entry, MemorySegment id) throws Throwable {
        return (int) IORegistryEntryGetRegistryEntryID.invokeExact(entry, id);
    }

    // kern_return_t IORegistryEntryGetName(io_registry_entry_t entry, io_name_t name);

    private static final MethodHandle IORegistryEntryGetName = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryGetName"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetName(MemorySegment entry, MemorySegment name) throws Throwable {
        return (int) IORegistryEntryGetName.invokeExact(entry, name);
    }

    // kern_return_t IORegistryEntryGetChildIterator(io_registry_entry_t entry, const io_name_t plane, io_iterator_t
    // *iterator);

    private static final MethodHandle IORegistryEntryGetChildIterator = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryGetChildIterator"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetChildIterator(MemorySegment entry, MemorySegment plane, MemorySegment iter)
            throws Throwable {
        return (int) IORegistryEntryGetChildIterator.invokeExact(entry, plane, iter);
    }

    // kern_return_t IORegistryEntryGetChildEntry(io_registry_entry_t entry, const io_name_t plane, io_registry_entry_t
    // *child);

    private static final MethodHandle IORegistryEntryGetChildEntry = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryGetChildEntry"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetChildEntry(MemorySegment entry, MemorySegment plane, MemorySegment child)
            throws Throwable {
        return (int) IORegistryEntryGetChildEntry.invokeExact(entry, plane, child);
    }

    // kern_return_t IORegistryEntryGetParentEntry(io_registry_entry_t entry, const io_name_t plane, io_registry_entry_t
    // *parent);

    private static final MethodHandle IORegistryEntryGetParentEntry = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryGetParentEntry"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetParentEntry(MemorySegment entry, MemorySegment plane, MemorySegment parent)
            throws Throwable {
        return (int) IORegistryEntryGetParentEntry.invokeExact(entry, plane, parent);
    }

    // CFTypeRef IORegistryEntryCreateCFProperty(io_registry_entry_t entry, CFStringRef key, CFAllocatorRef allocator,
    // IOOptionBits
    // options);

    private static final MethodHandle IORegistryEntryCreateCFProperty = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryCreateCFProperty"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    public static MemorySegment IORegistryEntryCreateCFProperty(MemorySegment entry, MemorySegment key,
            MemorySegment allocator, int options) throws Throwable {
        return (MemorySegment) IORegistryEntryCreateCFProperty.invokeExact(entry, key, allocator, options);
    }

    // kern_return_t IORegistryEntryCreateCFProperties(io_registry_entry_t entry, CFMutableDictionaryRef *properties,
    // CFAllocatorRef
    // allocator, IOOptionBits options);

    private static final MethodHandle IORegistryEntryCreateCFProperties = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntryCreateCFProperties"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    public static int IORegistryEntryCreateCFProperties(MemorySegment entry, MemorySegment properties,
            MemorySegment allocator, int options) throws Throwable {
        return (int) IORegistryEntryCreateCFProperties.invokeExact(entry, properties, allocator, options);
    }

    // CFTypeRef IORegistryEntrySearchCFProperty(io_registry_entry_t entry, const io_name_t plane, CFStringRef key,
    // CFAllocatorRef
    // allocator, IOOptionBits options);

    private static final MethodHandle IORegistryEntrySearchCFProperty = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IORegistryEntrySearchCFProperty"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    public static MemorySegment IORegistryEntrySearchCFProperty(MemorySegment entry, MemorySegment plane,
            MemorySegment key, MemorySegment allocator, int options) throws Throwable {
        return (MemorySegment) IORegistryEntrySearchCFProperty.invokeExact(entry, plane, key, allocator, options);
    }

    /*
     * IOPowerSources.h
     */

    // CFTypeRef IOPSCopyPowerSourcesInfo(void);

    private static final MethodHandle IOPSCopyPowerSourcesInfo = LINKER
            .downcallHandle(SYMBOL_LOOKUP.findOrThrow("IOPSCopyPowerSourcesInfo"), FunctionDescriptor.of(ADDRESS));

    public static MemorySegment IOPSCopyPowerSourcesInfo() throws Throwable {
        return (MemorySegment) IOPSCopyPowerSourcesInfo.invokeExact();
    }

    // CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

    private static final MethodHandle IOPSCopyPowerSourcesList = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOPSCopyPowerSourcesList"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOPSCopyPowerSourcesList(MemorySegment blob) throws Throwable {
        return (MemorySegment) IOPSCopyPowerSourcesList.invokeExact(blob);
    }

    // CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

    private static final MethodHandle IOPSGetPowerSourceDescription = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOPSGetPowerSourceDescription"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment IOPSGetPowerSourceDescription(MemorySegment blob, MemorySegment ps) throws Throwable {
        return (MemorySegment) IOPSGetPowerSourceDescription.invokeExact(blob, ps);
    }

    // CFTimeInterval IOPSGetTimeRemainingEstimate(void);

    private static final MethodHandle IOPSGetTimeRemainingEstimate = LINKER.downcallHandle(
            SYMBOL_LOOKUP.findOrThrow("IOPSGetTimeRemainingEstimate"), FunctionDescriptor.of(JAVA_DOUBLE));

    public static double IOPSGetTimeRemainingEstimate() throws Throwable {
        return (double) IOPSGetTimeRemainingEstimate.invokeExact();
    }

}
