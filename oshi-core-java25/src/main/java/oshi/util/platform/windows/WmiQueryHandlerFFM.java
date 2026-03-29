/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.com.ComObjectFFM;
import oshi.ffm.windows.com.FfmComException;
import oshi.ffm.windows.com.IEnumWbemClassObjectFFM;
import oshi.ffm.windows.com.IWbemLocatorFFM;
import oshi.ffm.windows.com.IWbemServicesFFM;
import oshi.ffm.windows.com.Ole32FFM;
import oshi.ffm.windows.com.WbemcliFFM;

import oshi.util.GlobalConfig;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * FFM-based utility to handle WMI Queries.
 * <p>
 * This class provides a high-level API for executing WMI queries using the Foreign Function and Memory API.
 * </p>
 */
@ThreadSafe
public class WmiQueryHandlerFFM {

    private static final Logger LOG = LoggerFactory.getLogger(WmiQueryHandlerFFM.class);

    // Global timeout from configuration, default -1 (infinite)
    private static final int GLOBAL_TIMEOUT = GlobalConfig.get(GlobalConfig.OSHI_UTIL_WMI_TIMEOUT, -1);

    static {
        if (GLOBAL_TIMEOUT == 0 || GLOBAL_TIMEOUT < -1) {
            throw new GlobalConfig.PropertyException(GlobalConfig.OSHI_UTIL_WMI_TIMEOUT);
        }
    }

    // Instance timeout (thread-safe)
    private final AtomicInteger wmiTimeout = new AtomicInteger(GLOBAL_TIMEOUT);

    // Cache failed WMI classes (thread-safe) - matches JNA behavior
    private final Set<String> failedWmiClassNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Preferred threading model (thread-safe)
    private final AtomicInteger comThreading = new AtomicInteger(Ole32FFM.COINIT_MULTITHREADED);

    // Track initialization of Security (thread-safe)
    private final AtomicBoolean securityInitialized = new AtomicBoolean(false);

    // Lock for COM initialization
    private final Object comInitLock = new Object();

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    // Factory to create this or a subclass
    private static Class<? extends WmiQueryHandlerFFM> customClass = null;

    protected WmiQueryHandlerFFM() {
    }

    /**
     * Factory method to create an instance of this class. To override this class, use {@link #setInstanceClass(Class)}
     * to define a subclass which extends {@link WmiQueryHandlerFFM}.
     *
     * @return an instance of this class or a class defined by {@link #setInstanceClass(Class)}
     */
    public static synchronized WmiQueryHandlerFFM createInstance() {
        if (customClass == null) {
            return new WmiQueryHandlerFFM();
        }
        try {
            return customClass.getConstructor(EMPTY_CLASS_ARRAY).newInstance(EMPTY_OBJECT_ARRAY);
        } catch (NoSuchMethodException | SecurityException e) {
            LOG.error("Failed to find or access a no-arg constructor for {}", customClass);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            LOG.error("Failed to create a new instance of {}", customClass);
        }
        return null;
    }

    /**
     * Define a subclass to be instantiated by {@link #createInstance()}. The class must extend
     * {@link WmiQueryHandlerFFM}.
     *
     * @param instanceClass the class to instantiate with {@link #createInstance()}
     */
    public static synchronized void setInstanceClass(Class<? extends WmiQueryHandlerFFM> instanceClass) {
        customClass = instanceClass;
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
            java.util.function.Supplier<T> resultFactory, TriConsumer<MemorySegment, Arena, T> rowProcessor) {

        // Check if class previously failed - skip to avoid repeated failures
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
                    Ole32FFM.CoSetProxyBlanket(pServices, -1, -1, Ole32FFM.RPC_C_AUTHN_LEVEL_CALL,
                            Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE, Ole32FFM.EOAC_NONE);

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
                            IEnumWbemClassObjectFFM.NextResult nextResult = IEnumWbemClassObjectFFM.next(pEnum,
                                    wmiTimeout.get(), arena);

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
        } catch (FfmComException e) {
            LOG.debug("COM exception querying {}: {}", wmiClassName, e.getMessage());
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
    public <T> List<T> queryWMI(String wmiClassName, String whereClause, java.util.function.Supplier<T> resultFactory,
            TriConsumer<MemorySegment, Arena, T> rowProcessor) {
        return queryWMI(WbemcliFFM.DEFAULT_NAMESPACE, wmiClassName, whereClause, resultFactory, rowProcessor);
    }

    /**
     * Initializes COM library.
     *
     * @return true if COM was initialized and needs to be uninitialized
     * @throws FfmComException if COM initialization fails with an unexpected error
     */
    public boolean initCOM() {
        boolean comInit = initCOM(comThreading.get());
        if (!comInit) {
            comInit = initCOM(switchComThreading());
        }

        if (comInit && !securityInitialized.get()) {
            synchronized (comInitLock) {
                // Double-check after acquiring lock
                if (!securityInitialized.get()) {
                    var hrOpt = Ole32FFM.CoInitializeSecurity(Ole32FFM.RPC_C_AUTHN_LEVEL_DEFAULT,
                            Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE, Ole32FFM.EOAC_NONE);

                    if (hrOpt.isPresent()) {
                        int hr = hrOpt.getAsInt();
                        // RPC_E_TOO_LATE is OK - security already initialized
                        if (Ole32FFM.succeeded(hr) || hr == Ole32FFM.RPC_E_TOO_LATE) {
                            securityInitialized.set(true);
                        } else {
                            Ole32FFM.CoUninitialize();
                            return false;
                        }
                    }
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
     * @throws FfmComException if COM initialization fails with an unexpected error
     */
    private boolean initCOM(int coInitThreading) {
        var hrOpt = Ole32FFM.CoInitializeEx(coInitThreading);
        if (hrOpt.isEmpty()) {
            return false;
        }
        int hr = hrOpt.getAsInt();
        return switch (hr) {
            // Successful initialization (S_OK) or already initialized (S_FALSE) but still needs uninit
            case Ole32FFM.S_OK, Ole32FFM.S_FALSE -> true;
            // COM already initialized with a different threading model
            case Ole32FFM.RPC_E_CHANGED_MODE -> false;
            // E_INVALIDARG, E_OUTOFMEMORY, or E_UNEXPECTED are possible per the docs
            default -> throw new FfmComException("Failed to initialize COM library.", hr);
        };
    }

    /**
     * Uninitializes COM library.
     */
    public void unInitCOM() {
        Ole32FFM.CoUninitialize();
    }

    /**
     * Switches the COM threading model.
     *
     * @return the new threading model
     */
    private int switchComThreading() {
        synchronized (comInitLock) {
            int current = comThreading.get();
            int newValue = (current == Ole32FFM.COINIT_APARTMENTTHREADED) ? Ole32FFM.COINIT_MULTITHREADED
                    : Ole32FFM.COINIT_APARTMENTTHREADED;
            comThreading.set(newValue);
            return newValue;
        }
    }

    /**
     * Gets the current WMI timeout in milliseconds.
     *
     * @return the current timeout (-1 for infinite)
     */
    public int getWmiTimeout() {
        return wmiTimeout.get();
    }

    /**
     * Sets the WMI timeout in milliseconds.
     *
     * @param wmiTimeout the timeout to set (-1 for infinite)
     */
    public void setWmiTimeout(int wmiTimeout) {
        this.wmiTimeout.set(wmiTimeout);
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
