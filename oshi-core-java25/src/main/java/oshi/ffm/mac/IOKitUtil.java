/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.mac;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.mac.MacSystemFunctions.mach_port_deallocate;
import static oshi.ffm.mac.MacSystemFunctions.mach_task_self;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import oshi.ffm.mac.IOKit.IOIterator;
import oshi.ffm.mac.IOKit.IORegistryEntry;
import oshi.ffm.mac.IOKit.IOService;

public final class IOKitUtil {

    private IOKitUtil() {
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
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment port = arena.allocate(JAVA_INT);
            int result = IOKitFunctions.IOMasterPort(0, port);
            if (result == 0) {
                return port.get(JAVA_INT, 0);
            }
            return 0;
        } catch (Throwable e) {
            return 0;
        }
    }

    /**
     * Gets the IO Registry root.
     *
     * @return a handle to the IORoot. Callers should release when finished.
     */
    public static IORegistryEntry getRoot() {
        try {
            int masterPort = getMasterPort();
            MemorySegment root = IOKitFunctions.IORegistryGetRootEntry(masterPort);
            deallocatePort(masterPort);
            return root.equals(MemorySegment.NULL) ? null : new IORegistryEntry(root);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Opens a the first IOService matching a service name.
     *
     * @param serviceName The service name to match
     * @return a handle to an IOService if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOService getMatchingService(String serviceName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameStr = arena.allocateFrom(serviceName);
            MemorySegment dict = IOKitFunctions.IOServiceMatching(nameStr);
            if (dict != null && !dict.equals(MemorySegment.NULL)) {
                return getMatchingService(dict);
            }
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Opens a the first IOService matching a dictionary.
     *
     * @param matchingDictionary The dictionary to match. This method will consume a reference to the dictionary.
     * @return a handle to an IOService if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOService getMatchingService(MemorySegment matchingDictionary) {
        try {
            int masterPort = getMasterPort();
            MemorySegment service = IOKitFunctions.IOServiceGetMatchingService(masterPort, matchingDictionary);
            deallocatePort(masterPort);
            return service.equals(MemorySegment.NULL) ? null : new IOService(service);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Convenience method to get IOService objects matching a service name.
     *
     * @param serviceName The service name to match
     * @return a handle to an IOIterator if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOIterator getMatchingServices(String serviceName) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nameStr = arena.allocateFrom(serviceName);
            MemorySegment dict = IOKitFunctions.IOServiceMatching(nameStr);
            if (dict != null && !dict.equals(MemorySegment.NULL)) {
                return getMatchingServices(dict);
            }
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Convenience method to get IOService objects matching a dictionary.
     *
     * @param matchingDictionary The dictionary to match. This method will consume a reference to the dictionary.
     * @return a handle to an IOIterator if successful, {@code null} if failed. Callers should release when finished.
     */
    public static IOIterator getMatchingServices(MemorySegment matchingDictionary) {
        try (Arena arena = Arena.ofConfined()) {
            int masterPort = getMasterPort();
            MemorySegment iteratorSeg = arena.allocate(ADDRESS);

            int result = IOKitFunctions.IOServiceGetMatchingServices(masterPort, matchingDictionary, iteratorSeg);
            deallocatePort(masterPort);

            if (result == 0) {
                MemorySegment iterator = iteratorSeg.get(ADDRESS, 0);
                if (!iterator.equals(MemorySegment.NULL)) {
                    return new IOIterator(iterator);
                }
            }
            return null;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Convenience method to get the IO dictionary matching a bsd name.
     *
     * @param bsdName The bsd name of the registry entry
     * @return The dictionary ref if successful, {@code null} if failed. Callers should release when finished.
     */
    public static MemorySegment getBSDNameMatchingDict(String bsdName) {
        try (Arena arena = Arena.ofConfined()) {
            int masterPort = getMasterPort();
            MemorySegment bsdNameStr = arena.allocateFrom(bsdName);
            MemorySegment result = IOKitFunctions.IOBSDNameMatching(masterPort, 0, bsdNameStr);
            deallocatePort(masterPort);
            return result;
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * Deallocate a port
     *
     * @param port the port to deallocate
     */
    private static void deallocatePort(int port) {
        try {
            if (port != 0) {
                mach_port_deallocate(mach_task_self(), port);
            }
        } catch (Throwable e) {
            // Ignore
        }
    }
}
