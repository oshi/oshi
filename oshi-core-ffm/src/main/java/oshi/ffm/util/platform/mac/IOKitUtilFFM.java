/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.ForeignFunctions.callInArenaIntOrDefault;
import static oshi.ffm.ForeignFunctions.callInArenaOrDefault;
import static oshi.ffm.platform.mac.IOKitFunctions.IOBSDNameMatching;
import static oshi.ffm.platform.mac.IOKitFunctions.IOMasterPort;
import static oshi.ffm.platform.mac.IOKitFunctions.IORegistryGetRootEntry;
import static oshi.ffm.platform.mac.IOKitFunctions.IOServiceGetMatchingService;
import static oshi.ffm.platform.mac.IOKitFunctions.IOServiceGetMatchingServices;
import static oshi.ffm.platform.mac.IOKitFunctions.IOServiceMatching;
import static oshi.ffm.platform.mac.MacSystemFunctions.mach_port_deallocate;
import static oshi.ffm.platform.mac.MacSystemFunctions.mach_task_self;
import static oshi.util.ExceptionUtil.getOrDefault;
import static oshi.util.ExceptionUtil.runSilently;
import static oshi.util.LogLevel.TRACE;

import java.lang.foreign.MemorySegment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.mac.IOKit.IOIterator;
import oshi.ffm.platform.mac.IOKit.IORegistryEntry;
import oshi.ffm.platform.mac.IOKit.IOService;

/**
 * FFM-based utility for macOS IOKit registry and service operations.
 */
public final class IOKitUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(IOKitUtilFFM.class);

    private IOKitUtilFFM() {
    }

    /**
     * Gets a pointer to the Mach Master Port.
     *
     * @return The master port.
     *         <p>
     *         Multiple calls to {@link #getMasterPort} will not result in leaking ports but it is considered good
     *         programming practice to deallocate the port when you are finished with it, using mach_port_deallocate.
     */
    public static int getMasterPort() {
        return callInArenaIntOrDefault(arena -> {
            MemorySegment port = arena.allocate(JAVA_INT);
            int result = IOMasterPort(0, port);
            if (result == 0) {
                return port.get(JAVA_INT, 0);
            }
            return 0;
        }, LOG, TRACE, "Failed to get IOKit master port", 0);
    }

    /**
     * Gets the IO Registry root.
     *
     * @return a handle to the IORoot. Callers should release when finished.
     */
    public static IORegistryEntry getRoot() {
        return getOrDefault(() -> {
            int masterPort = getMasterPort();
            MemorySegment root = IORegistryGetRootEntry(masterPort);
            deallocatePort(masterPort);
            return root.equals(MemorySegment.NULL) ? null : new IORegistryEntry(root);
        }, null);
    }

    /**
     * Opens a the first IOService matching a service name.
     *
     * @param serviceName The service name to match
     * @return a handle to an IOService if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOService getMatchingService(String serviceName) {
        return callInArenaOrDefault(arena -> {
            MemorySegment nameStr = arena.allocateFrom(serviceName);
            MemorySegment dict = IOServiceMatching(nameStr);
            if (dict != null && !dict.equals(MemorySegment.NULL)) {
                return getMatchingService(dict);
            }
            return null;
        }, LOG, TRACE, "Failed to get matching IOService for name", null);
    }

    /**
     * Opens a the first IOService matching a dictionary.
     *
     * @param matchingDictionary The dictionary to match. This method will consume a reference to the dictionary.
     * @return a handle to an IOService if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOService getMatchingService(MemorySegment matchingDictionary) {
        return getOrDefault(() -> {
            int masterPort = getMasterPort();
            MemorySegment service = IOServiceGetMatchingService(masterPort, matchingDictionary);
            deallocatePort(masterPort);
            return service.equals(MemorySegment.NULL) ? null : new IOService(service);
        }, null);
    }

    /**
     * Convenience method to get IOService objects matching a service name.
     *
     * @param serviceName The service name to match
     * @return a handle to an IOIterator if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOIterator getMatchingServices(String serviceName) {
        return callInArenaOrDefault(arena -> {
            MemorySegment nameStr = arena.allocateFrom(serviceName);
            MemorySegment dict = IOServiceMatching(nameStr);
            if (dict != null && !dict.equals(MemorySegment.NULL)) {
                return getMatchingServices(dict);
            }
            return null;
        }, LOG, TRACE, "Failed to get matching IOServices for name", null);
    }

    /**
     * Convenience method to get IOService objects matching a dictionary.
     *
     * @param matchingDictionary The dictionary to match. This method will consume a reference to the dictionary.
     * @return a handle to an IOIterator if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOIterator getMatchingServices(MemorySegment matchingDictionary) {
        return callInArenaOrDefault(arena -> {
            int masterPort = getMasterPort();
            MemorySegment iteratorSeg = arena.allocate(ADDRESS);

            int result = IOServiceGetMatchingServices(masterPort, matchingDictionary, iteratorSeg);
            deallocatePort(masterPort);

            if (result == 0) {
                MemorySegment iterator = iteratorSeg.get(ADDRESS, 0);
                if (!iterator.equals(MemorySegment.NULL)) {
                    return new IOIterator(iterator);
                }
            }
            return null;
        }, LOG, TRACE, "Failed to get matching IOServices for dictionary", null);
    }

    /**
     * Convenience method to get the IO dictionary matching a bsd name.
     *
     * @param bsdName The bsd name of the registry entry
     * @return The dictionary ref if successful, {@code null} if failed. Callers should release when finished.
     */
    public static MemorySegment getBSDNameMatchingDict(String bsdName) {
        return callInArenaOrDefault(arena -> {
            int masterPort = getMasterPort();
            MemorySegment bsdNameStr = arena.allocateFrom(bsdName);
            MemorySegment result = IOBSDNameMatching(masterPort, 0, bsdNameStr);
            deallocatePort(masterPort);
            return result;
        }, LOG, TRACE, "Failed to get BSD name matching dictionary", null);
    }

    /**
     * Deallocate a port
     *
     * @param port the port to deallocate
     */
    private static void deallocatePort(int port) {
        runSilently(() -> {
            if (port != 0) {
                mach_port_deallocate(mach_task_self(), port);
            }
        });
    }
}
