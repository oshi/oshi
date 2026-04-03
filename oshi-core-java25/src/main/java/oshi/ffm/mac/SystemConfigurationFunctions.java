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

import oshi.ffm.ForeignFunctions;

public final class SystemConfigurationFunctions extends ForeignFunctions {

    private SystemConfigurationFunctions() {
    }

    private static final SymbolLookup SC_LIBRARY = frameworkLookup("SystemConfiguration");

    // CFArrayRef SCNetworkInterfaceCopyAll(void);

    private static final MethodHandle SCNetworkInterfaceCopyAll = LINKER
            .downcallHandle(SC_LIBRARY.findOrThrow("SCNetworkInterfaceCopyAll"), FunctionDescriptor.of(ADDRESS));

    public static MemorySegment SCNetworkInterfaceCopyAll() throws Throwable {
        return (MemorySegment) SCNetworkInterfaceCopyAll.invokeExact();
    }

    // CFStringRef SCNetworkInterfaceGetBSDName(SCNetworkInterfaceRef interface);

    private static final MethodHandle SCNetworkInterfaceGetBSDName = LINKER.downcallHandle(
            SC_LIBRARY.findOrThrow("SCNetworkInterfaceGetBSDName"), FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment SCNetworkInterfaceGetBSDName(MemorySegment netif) throws Throwable {
        return (MemorySegment) SCNetworkInterfaceGetBSDName.invokeExact(netif);
    }

    // CFStringRef SCNetworkInterfaceGetLocalizedDisplayName(SCNetworkInterfaceRef interface);

    private static final MethodHandle SCNetworkInterfaceGetLocalizedDisplayName = LINKER.downcallHandle(
            SC_LIBRARY.findOrThrow("SCNetworkInterfaceGetLocalizedDisplayName"),
            FunctionDescriptor.of(ADDRESS, ADDRESS));

    public static MemorySegment SCNetworkInterfaceGetLocalizedDisplayName(MemorySegment netif) throws Throwable {
        return (MemorySegment) SCNetworkInterfaceGetLocalizedDisplayName.invokeExact(netif);
    }
}
