/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

public final class IOKitFunctions extends MacForeignFunctions {

    private IOKitFunctions() {
    }

    private static final SymbolLookup IOKIT_LIBRARY = frameworkLookup("IOKit");

    /*
     * IOKitLib.h
     */

    // kern_return_t IOMasterPort(mach_port_t bootstrapPort, mach_port_t *mainPort);

    private static final MethodHandle IOMasterPort = LINKER.downcallHandle(IOKIT_LIBRARY.findOrThrow("IOMasterPort"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));

    public static int IOMasterPort(int bootstrapPort, MemorySegment port) throws Throwable {
        return (int) IOMasterPort.invokeExact(bootstrapPort, port);
    }

    // io_registry_entry_t IORegistryGetRootEntry(mach_port_t mainPort);

    private static final MethodHandle IORegistryGetRootEntry = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryGetRootEntry"), FunctionDescriptor.of(ADDRESS, JAVA_INT));

    public static MemorySegment IORegistryGetRootEntry(int masterPort) throws Throwable {
        return (MemorySegment) IORegistryGetRootEntry.invokeExact(masterPort);
    }

    // CFMutableDictionaryRef IOServiceNameMatching(const char *name);

    private static final MethodHandle IOServiceNameMatching = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOServiceNameMatching"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOServiceNameMatching(MemorySegment name) throws Throwable {
        return (MemorySegment) IOServiceNameMatching.invokeExact(name);
    }

    // CFMutableDictionaryRef IOServiceMatching(const char *name);

    private static final MethodHandle IOServiceMatching = LINKER
            .downcallHandle(IOKIT_LIBRARY.findOrThrow("IOServiceMatching"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOServiceMatching(MemorySegment name) throws Throwable {
        return (MemorySegment) IOServiceMatching.invokeExact(name);
    }

    // io_service_t IOServiceGetMatchingService(mach_port_t mainPort, CFDictionaryRef matching);

    private static final MethodHandle IOServiceGetMatchingService = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOServiceGetMatchingService"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS));

    public static MemorySegment IOServiceGetMatchingService(int masterPort, MemorySegment matchingDict)
            throws Throwable {
        return (MemorySegment) IOServiceGetMatchingService.invokeExact(masterPort, matchingDict);
    }

    // kern_return_t IOServiceGetMatchingServices(mach_port_t mainPort, CFDictionaryRef matching, io_iterator_t
    // *existing);

    private static final MethodHandle IOServiceGetMatchingServices = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOServiceGetMatchingServices"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS));

    public static int IOServiceGetMatchingServices(int masterPort, MemorySegment matchingDict, MemorySegment iterator)
            throws Throwable {
        return (int) IOServiceGetMatchingServices.invokeExact(masterPort, matchingDict, iterator);
    }

    // CFMutableDictionaryRef IOBSDNameMatching(mach_port_t mainPort, uint32_t options, const char *bsdName);

    private static final MethodHandle IOBSDNameMatching = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOBSDNameMatching"),
            FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    public static MemorySegment IOBSDNameMatching(int masterPort, int options, MemorySegment bsdName) throws Throwable {
        return (MemorySegment) IOBSDNameMatching.invokeExact(masterPort, options, bsdName);
    }

    // kern_return_t IOObjectRelease(io_object_t object);

    private static final MethodHandle IOObjectRelease = LINKER
            .downcallHandle(IOKIT_LIBRARY.findOrThrow("IOObjectRelease"), FunctionDescriptor.of(JAVA_INT, ADDRESS));

    public static int IOObjectRelease(MemorySegment object) throws Throwable {
        return (int) IOObjectRelease.invokeExact(object);
    }

    // boolean_t IOObjectConformsTo(io_object_t object, const io_name_t className);

    private static final MethodHandle IOObjectConformsTo = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOObjectConformsTo"), FunctionDescriptor.of(JAVA_BOOLEAN, ADDRESS, ADDRESS));

    public static boolean IOObjectConformsTo(MemorySegment object, MemorySegment className) throws Throwable {
        return (boolean) IOObjectConformsTo.invokeExact(object, className);
    }

    // io_object_t IOIteratorNext(io_iterator_t iterator);

    private static final MethodHandle IOIteratorNext = LINKER
            .downcallHandle(IOKIT_LIBRARY.findOrThrow("IOIteratorNext"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOIteratorNext(MemorySegment iterator) throws Throwable {
        return (MemorySegment) IOIteratorNext.invokeExact(iterator);
    }

    // kern_return_t IORegistryEntryGetRegistryEntryID(io_registry_entry_t entry, uint64_t *entryID);

    private static final MethodHandle IORegistryEntryGetRegistryEntryID = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryGetRegistryEntryID"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetRegistryEntryID(MemorySegment entry, MemorySegment id) throws Throwable {
        return (int) IORegistryEntryGetRegistryEntryID.invokeExact(entry, id);
    }

    // kern_return_t IORegistryEntryGetName(io_registry_entry_t entry, io_name_t name);

    private static final MethodHandle IORegistryEntryGetName = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryGetName"), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetName(MemorySegment entry, MemorySegment name) throws Throwable {
        return (int) IORegistryEntryGetName.invokeExact(entry, name);
    }

    // kern_return_t IORegistryEntryGetChildIterator(io_registry_entry_t entry, const io_name_t plane, io_iterator_t
    // *iterator);

    private static final MethodHandle IORegistryEntryGetChildIterator = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryGetChildIterator"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetChildIterator(MemorySegment entry, MemorySegment plane, MemorySegment iter)
            throws Throwable {
        return (int) IORegistryEntryGetChildIterator.invokeExact(entry, plane, iter);
    }

    // kern_return_t IORegistryEntryGetChildEntry(io_registry_entry_t entry, const io_name_t plane, io_registry_entry_t
    // *child);

    private static final MethodHandle IORegistryEntryGetChildEntry = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryGetChildEntry"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetChildEntry(MemorySegment entry, MemorySegment plane, MemorySegment child)
            throws Throwable {
        return (int) IORegistryEntryGetChildEntry.invokeExact(entry, plane, child);
    }

    // kern_return_t IORegistryEntryGetParentEntry(io_registry_entry_t entry, const io_name_t plane, io_registry_entry_t
    // *parent);

    private static final MethodHandle IORegistryEntryGetParentEntry = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryGetParentEntry"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

    public static int IORegistryEntryGetParentEntry(MemorySegment entry, MemorySegment plane, MemorySegment parent)
            throws Throwable {
        return (int) IORegistryEntryGetParentEntry.invokeExact(entry, plane, parent);
    }

    // CFTypeRef IORegistryEntryCreateCFProperty(io_registry_entry_t entry, CFStringRef key, CFAllocatorRef allocator,
    // IOOptionBits
    // options);

    private static final MethodHandle IORegistryEntryCreateCFProperty = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryCreateCFProperty"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    public static MemorySegment IORegistryEntryCreateCFProperty(MemorySegment entry, MemorySegment key,
            MemorySegment allocator, int options) throws Throwable {
        return (MemorySegment) IORegistryEntryCreateCFProperty.invokeExact(entry, key, allocator, options);
    }

    // kern_return_t IORegistryEntryCreateCFProperties(io_registry_entry_t entry, CFMutableDictionaryRef *properties,
    // CFAllocatorRef
    // allocator, IOOptionBits options);

    private static final MethodHandle IORegistryEntryCreateCFProperties = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntryCreateCFProperties"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT));

    public static int IORegistryEntryCreateCFProperties(MemorySegment entry, MemorySegment properties,
            MemorySegment allocator, int options) throws Throwable {
        return (int) IORegistryEntryCreateCFProperties.invokeExact(entry, properties, allocator, options);
    }

    // CFTypeRef IORegistryEntrySearchCFProperty(io_registry_entry_t entry, const io_name_t plane, CFStringRef key,
    // CFAllocatorRef
    // allocator, IOOptionBits options);

    private static final MethodHandle IORegistryEntrySearchCFProperty = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IORegistryEntrySearchCFProperty"),
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
            .downcallHandle(IOKIT_LIBRARY.findOrThrow("IOPSCopyPowerSourcesInfo"), FunctionDescriptor.of(ADDRESS));

    public static MemorySegment IOPSCopyPowerSourcesInfo() throws Throwable {
        return (MemorySegment) IOPSCopyPowerSourcesInfo.invokeExact();
    }

    // CFArrayRef IOPSCopyPowerSourcesList(CFTypeRef blob);

    private static final MethodHandle IOPSCopyPowerSourcesList = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOPSCopyPowerSourcesList"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment IOPSCopyPowerSourcesList(MemorySegment blob) throws Throwable {
        return (MemorySegment) IOPSCopyPowerSourcesList.invokeExact(blob);
    }

    // CFDictionaryRef IOPSGetPowerSourceDescription(CFTypeRef blob, CFTypeRef ps);

    private static final MethodHandle IOPSGetPowerSourceDescription = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOPSGetPowerSourceDescription"),
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));

    public static MemorySegment IOPSGetPowerSourceDescription(MemorySegment blob, MemorySegment ps) throws Throwable {
        return (MemorySegment) IOPSGetPowerSourceDescription.invokeExact(blob, ps);
    }

    // CFTimeInterval IOPSGetTimeRemainingEstimate(void);

    private static final MethodHandle IOPSGetTimeRemainingEstimate = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOPSGetTimeRemainingEstimate"), FunctionDescriptor.of(JAVA_DOUBLE));

    public static double IOPSGetTimeRemainingEstimate() throws Throwable {
        return (double) IOPSGetTimeRemainingEstimate.invokeExact();
    }

    /*
     * IOKitLib.h - IOConnect / IOService open/close
     */

    // kern_return_t IOServiceOpen(io_service_t service, task_port_t owningTask, uint32_t type,
    // io_connect_t *connect);

    private static final MethodHandle IOServiceOpen = LINKER.downcallHandle(IOKIT_LIBRARY.findOrThrow("IOServiceOpen"),
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS));

    public static int IOServiceOpen(MemorySegment service, int owningTask, int type, MemorySegment connect)
            throws Throwable {
        return (int) IOServiceOpen.invokeExact(service, owningTask, type, connect);
    }

    // kern_return_t IOServiceClose(io_connect_t connect);

    private static final MethodHandle IOServiceClose = LINKER
            .downcallHandle(IOKIT_LIBRARY.findOrThrow("IOServiceClose"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));

    public static int IOServiceClose(int connect) throws Throwable {
        return (int) IOServiceClose.invokeExact(connect);
    }

    // kern_return_t IOConnectCallStructMethod(io_connect_t connection, uint32_t selector,
    // const void *inputStruct, size_t inputStructCnt,
    // void *outputStruct, size_t *outputStructCnt);

    private static final MethodHandle IOConnectCallStructMethod = LINKER.downcallHandle(
            IOKIT_LIBRARY.findOrThrow("IOConnectCallStructMethod"),
            FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, ADDRESS));

    public static int IOConnectCallStructMethod(int connection, int selector, MemorySegment inputStruct,
            long inputStructCnt, MemorySegment outputStruct, MemorySegment outputStructCnt) throws Throwable {
        return (int) IOConnectCallStructMethod.invokeExact(connection, selector, inputStruct, inputStructCnt,
                outputStruct, outputStructCnt);
    }

}
