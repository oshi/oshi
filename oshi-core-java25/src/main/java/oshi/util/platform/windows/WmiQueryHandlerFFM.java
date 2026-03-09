/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.com.BStrFFM;
import oshi.ffm.windows.com.ComObjectFFM;
import oshi.ffm.windows.com.IEnumWbemClassObjectFFM;
import oshi.ffm.windows.com.IWbemClassObjectFFM;
import oshi.ffm.windows.com.IWbemLocatorFFM;
import oshi.ffm.windows.com.IWbemServicesFFM;
import oshi.ffm.windows.com.Ole32FFM;
import oshi.ffm.windows.com.WbemcliFFM;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.lang.foreign.MemorySegment.NULL;

/**
 * FFM-based utility to handle WMI Queries.
 * <p>
 * This class provides a high-level API for executing WMI queries using the Foreign Function and Memory API.
 * </p>
 */
@ThreadSafe
public class WmiQueryHandlerFFM {

    private static final Logger LOG = LoggerFactory.getLogger(WmiQueryHandlerFFM.class);

    // Track failed WMI classes to avoid repeated failures
    private final Set<String> failedWmiClassNames = new HashSet<>();

    // Preferred threading model
    private int comThreading = Ole32FFM.COINIT_MULTITHREADED;

    // Track initialization of Security
    private boolean securityInitialized = false;

    private static final WmiQueryHandlerFFM INSTANCE = new WmiQueryHandlerFFM();

    private WmiQueryHandlerFFM() {
    }

    /**
     * Gets the singleton instance of the WMI query handler.
     *
     * @return the singleton instance
     */
    public static WmiQueryHandlerFFM getInstance() {
        return INSTANCE;
    }

    /**
     * Executes a WMI query and processes each result row.
     *
     * @param <T>           the type of result object
     * @param namespace     the WMI namespace (e.g., "ROOT\\CIMV2")
     * @param wmiClassName  the WMI class name (e.g., "Win32_LogicalDisk")
     * @param whereClause   optional WHERE clause (null for none)
     * @param resultFactory factory to create result objects
     * @param rowProcessor  processor to populate result object from WMI row
     * @return list of result objects, empty list if query fails
     */
    public <T> List<T> queryWMI(String namespace, String wmiClassName, String whereClause,
            java.util.function.Supplier<T> resultFactory,
            TriConsumer<MemorySegment, Arena, T> rowProcessor) {

        if (failedWmiClassNames.contains(wmiClassName)) {
            return new ArrayList<>();
        }

        List<T> results = new ArrayList<>();
        boolean comInit = false;

        try (Arena arena = Arena.ofConfined()) {
            // Initialize COM
            comInit = initCOM();
            if (!comInit) {
                LOG.debug("Failed to initialize COM");
                return results;
            }

            // Create WbemLocator
            Optional<MemorySegment> pLocatorOpt = IWbemLocatorFFM.create(arena);
            if (pLocatorOpt.isEmpty()) {
                LOG.debug("Failed to create IWbemLocator");
                return results;
            }
            MemorySegment pLocator = pLocatorOpt.get();

            try {
                // Connect to namespace
                Optional<MemorySegment> pServicesOpt = IWbemLocatorFFM.connectServer(pLocator, namespace, arena);
                if (pServicesOpt.isEmpty()) {
                    LOG.debug("Failed to connect to namespace: {}", namespace);
                    failedWmiClassNames.add(wmiClassName);
                    return results;
                }
                MemorySegment pServices = pServicesOpt.get();

                try {
                    // Set proxy blanket for security
                    Ole32FFM.CoSetProxyBlanket(pServices, -1, -1,
                            Ole32FFM.RPC_C_AUTHN_LEVEL_CALL,
                            Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE,
                            Ole32FFM.EOAC_NONE);

                    // Build query
                    String query = "SELECT * FROM " + wmiClassName;
                    if (whereClause != null && !whereClause.isEmpty()) {
                        query += " " + whereClause;
                    }

                    // Execute query
                    Optional<MemorySegment> pEnumOpt = IWbemServicesFFM.execQuery(pServices, query, arena);
                    if (pEnumOpt.isEmpty()) {
                        LOG.debug("Failed to execute query: {}", query);
                        failedWmiClassNames.add(wmiClassName);
                        return results;
                    }
                    MemorySegment pEnum = pEnumOpt.get();

                    try {
                        // Enumerate results
                        while (true) {
                            IEnumWbemClassObjectFFM.NextResult nextResult = IEnumWbemClassObjectFFM.next(pEnum, arena);

                            if (nextResult.isComplete() || !nextResult.hasObject()) {
                                break;
                            }

                            MemorySegment pObject = nextResult.pObject();
                            try {
                                T result = resultFactory.get();
                                rowProcessor.accept(pObject, arena, result);
                                results.add(result);
                            } finally {
                                ComObjectFFM.safeRelease(pObject, arena);
                            }
                        }
                    } finally {
                        ComObjectFFM.safeRelease(pEnum, arena);
                    }
                } finally {
                    ComObjectFFM.safeRelease(pServices, arena);
                }
            } finally {
                ComObjectFFM.safeRelease(pLocator, arena);
            }
        } catch (Exception e) {
            LOG.debug("WMI query failed for {}: {}", wmiClassName, e.getMessage());
            failedWmiClassNames.add(wmiClassName);
        } finally {
            if (comInit) {
                unInitCOM();
            }
        }

        return results;
    }

    /**
     * Executes a WMI query in the default CIMV2 namespace.
     *
     * @param <T>           the type of result object
     * @param wmiClassName  the WMI class name
     * @param whereClause   optional WHERE clause
     * @param resultFactory factory to create result objects
     * @param rowProcessor  processor to populate result object from WMI row
     * @return list of result objects
     */
    public <T> List<T> queryWMI(String wmiClassName, String whereClause,
            java.util.function.Supplier<T> resultFactory,
            TriConsumer<MemorySegment, Arena, T> rowProcessor) {
        return queryWMI(WbemcliFFM.DEFAULT_NAMESPACE, wmiClassName, whereClause, resultFactory, rowProcessor);
    }

    /**
     * Initializes COM library.
     *
     * @return true if COM was initialized and needs to be uninitialized
     */
    private boolean initCOM() {
        boolean comInit = initCOM(comThreading);
        if (!comInit) {
            comInit = initCOM(switchComThreading());
        }

        if (comInit && !securityInitialized) {
            var hrOpt = Ole32FFM.CoInitializeSecurity(
                    Ole32FFM.RPC_C_AUTHN_LEVEL_DEFAULT,
                    Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE,
                    Ole32FFM.EOAC_NONE);

            if (hrOpt.isPresent()) {
                int hr = hrOpt.getAsInt();
                // RPC_E_TOO_LATE is OK - security already initialized
                if (Ole32FFM.succeeded(hr) || hr == Ole32FFM.RPC_E_TOO_LATE) {
                    securityInitialized = true;
                } else {
                    Ole32FFM.CoUninitialize();
                    return false;
                }
            }
        }
        return comInit;
    }

    /**
     * Initializes COM with a specific threading model.
     *
     * @param coInitThreading the threading model
     * @return true if successful
     */
    private boolean initCOM(int coInitThreading) {
        var hrOpt = Ole32FFM.CoInitializeEx(coInitThreading);
        if (hrOpt.isEmpty()) {
            return false;
        }
        int hr = hrOpt.getAsInt();
        return switch (hr) {
            case Ole32FFM.S_OK, Ole32FFM.S_FALSE -> true;
            case Ole32FFM.RPC_E_CHANGED_MODE -> false;
            default -> false;
        };
    }

    /**
     * Uninitializes COM library.
     */
    private void unInitCOM() {
        Ole32FFM.CoUninitialize();
    }

    /**
     * Switches the COM threading model.
     *
     * @return the new threading model
     */
    private int switchComThreading() {
        if (comThreading == Ole32FFM.COINIT_APARTMENTTHREADED) {
            comThreading = Ole32FFM.COINIT_MULTITHREADED;
        } else {
            comThreading = Ole32FFM.COINIT_APARTMENTTHREADED;
        }
        return comThreading;
    }

    /**
     * Functional interface for processing WMI rows with three parameters.
     *
     * @param <T> the result type
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }
}
