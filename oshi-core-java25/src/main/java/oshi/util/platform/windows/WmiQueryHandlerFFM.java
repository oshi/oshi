/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.platform.windows;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.com.FfmComException;
import oshi.ffm.windows.com.IEnumWbemClassObjectFFM;
import oshi.ffm.windows.com.IUnknownFFM;
import oshi.ffm.windows.com.IWbemLocatorFFM;
import oshi.ffm.windows.com.IWbemServicesFFM;
import oshi.ffm.windows.com.Ole32FFM;
import oshi.ffm.windows.com.WbemcliFFM;
import oshi.util.GlobalConfig;

/**
 * FFM-based utility to handle WMI Queries. Designed to mirror the JNA {@code WmiQueryHandler} API.
 */
@ThreadSafe
public class WmiQueryHandlerFFM {

    private static final Logger LOG = LoggerFactory.getLogger(WmiQueryHandlerFFM.class);

    private static int globalTimeout = GlobalConfig.get(GlobalConfig.OSHI_UTIL_WMI_TIMEOUT, -1);

    static {
        if (globalTimeout == 0 || globalTimeout < -1) {
            throw new GlobalConfig.PropertyException(GlobalConfig.OSHI_UTIL_WMI_TIMEOUT);
        }
    }

    // Timeout for WMI queries
    private volatile int wmiTimeout = globalTimeout;

    // Cache failed wmi classes
    private final Set<String> failedWmiClassNames = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Preferred threading model
    private int comThreading = Ole32FFM.COINIT_MULTITHREADED;

    // Track initialization of Security
    private volatile boolean securityInitialized = false;

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
     * @return An instance of this class or a class defined by {@link #setInstanceClass(Class)}
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
        customClass = null;
        return new WmiQueryHandlerFFM();
    }

    /**
     * Define a subclass to be instantiated by {@link #createInstance()}. The class must extend
     * {@link WmiQueryHandlerFFM}.
     *
     * @param instanceClass The class to instantiate with {@link #createInstance()}.
     */
    public static synchronized void setInstanceClass(Class<? extends WmiQueryHandlerFFM> instanceClass) {
        customClass = instanceClass;
    }

    /**
     * Query WMI for values. Makes no assumptions on whether the user has previously initialized COM.
     *
     * @param <T>   WMI queries use an Enum to identify the fields to query, and use the enum values as keys to retrieve
     *              the results.
     * @param query A WmiQuery object encapsulating the namespace, class, and properties
     * @return a WmiResult object containing the query results, wrapping an EnumMap
     */
    public <T extends Enum<T>> WbemcliUtilFFM.WmiResult<T> queryWMI(WbemcliUtilFFM.WmiQuery<T> query) {
        return queryWMI(query, true);
    }

    /**
     * Query WMI for values.
     *
     * @param <T>     WMI queries use an Enum to identify the fields to query, and use the enum values as keys to
     *                retrieve the results.
     * @param query   A WmiQuery object encapsulating the namespace, class, and properties
     * @param initCom Whether to initialize COM. If {@code true}, initializes COM before the query and uninitializes
     *                after. If {@code false}, assumes the caller has already called {@link #initCOM()} and will call
     *                {@link #unInitCOM()} when done. This can improve WMI query performance.
     * @return a WmiResult object containing the query results, wrapping an EnumMap
     */
    public <T extends Enum<T>> WbemcliUtilFFM.WmiResult<T> queryWMI(WbemcliUtilFFM.WmiQuery<T> query, boolean initCom) {
        WbemcliUtilFFM.WmiResult<T> result = new WbemcliUtilFFM.WmiResult<>(query.getPropertyEnum());
        if (failedWmiClassNames.contains(query.getWmiClassName())) {
            return result;
        }
        boolean comInit = false;
        try (Arena arena = Arena.ofConfined()) {
            if (initCom) {
                comInit = initCOM();
                if (!comInit) {
                    return result;
                }
            }

            Optional<MemorySegment> pLocatorOpt = IWbemLocatorFFM.create(arena);
            if (pLocatorOpt.isEmpty()) {
                LOG.debug("Failed to create IWbemLocator. COM may not be initialized.");
                return result;
            }
            MemorySegment pLocator = pLocatorOpt.get();

            try {
                Optional<MemorySegment> pServicesOpt = IWbemLocatorFFM.connectServer(pLocator, query.getNameSpace(),
                        arena);
                if (pServicesOpt.isEmpty()) {
                    failedWmiClassNames.add(query.getWmiClassName());
                    return result;
                }
                MemorySegment pServices = pServicesOpt.get();

                try {
                    Ole32FFM.CoSetProxyBlanket(pServices, -1, -1, Ole32FFM.RPC_C_AUTHN_LEVEL_CALL,
                            Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE, Ole32FFM.EOAC_NONE);

                    // Build WQL query: SELECT prop1,prop2,... FROM ClassName [WHERE ...]
                    T[] props = query.getPropertyEnum().getEnumConstants();
                    StringBuilder sb = new StringBuilder("SELECT ");
                    sb.append(props[0].name());
                    for (int i = 1; i < props.length; i++) {
                        sb.append(',').append(props[i].name());
                    }
                    sb.append(" FROM ").append(query.getWmiClassName());

                    Optional<MemorySegment> pEnumOpt = IWbemServicesFFM.execQuery(pServices, sb.toString(), arena);
                    if (pEnumOpt.isEmpty()) {
                        failedWmiClassNames.add(query.getWmiClassName());
                        return result;
                    }
                    MemorySegment pEnum = pEnumOpt.get();

                    try {
                        while (true) {
                            IEnumWbemClassObjectFFM.NextResult nextResult = IEnumWbemClassObjectFFM.next(pEnum,
                                    wmiTimeout, arena);
                            if (nextResult.isComplete() || !nextResult.hasObject()) {
                                break;
                            }
                            MemorySegment pObject = nextResult.pObject();
                            try {
                                WbemcliUtilFFM.populateRow(pObject, arena, result);
                            } finally {
                                IUnknownFFM.safeRelease(pObject, arena);
                            }
                        }
                    } finally {
                        IUnknownFFM.safeRelease(pEnum, arena);
                    }
                } finally {
                    IUnknownFFM.safeRelease(pServices, arena);
                }
            } finally {
                IUnknownFFM.safeRelease(pLocator, arena);
            }
        } catch (FfmComException e) {
            int hresult = e.getHresult();
            switch (hresult) {
                case WbemcliFFM.WBEM_E_INVALID_NAMESPACE:
                    LOG.warn("COM exception: Invalid Namespace {}", query.getNameSpace());
                    break;
                case WbemcliFFM.WBEM_E_INVALID_CLASS:
                    LOG.warn("COM exception: Invalid Class {}", query.getWmiClassName());
                    break;
                case WbemcliFFM.WBEM_E_INVALID_QUERY:
                    LOG.warn("COM exception: Invalid Query for {}", query.getWmiClassName());
                    break;
                default:
                    handleComException(query, e);
                    break;
            }
            failedWmiClassNames.add(query.getWmiClassName());
        } catch (Exception e) {
            LOG.debug("WMI query failed for {}: {}", query.getWmiClassName(), e.getMessage());
        } finally {
            if (comInit) {
                unInitCOM();
            }
        }
        return result;
    }

    /**
     * Executes a WMI query in the default namespace, processing each result row with a custom processor.
     *
     * @param <T>           the type of result object
     * @param wmiClassName  the WMI class name
     * @param whereClause   optional WHERE clause (null for none)
     * @param resultFactory factory to create result objects per row
     * @param rowProcessor  processor to populate result object from WMI row
     * @return list of result objects, empty list if query fails
     */
    public <T> List<T> queryWMI(String wmiClassName, String whereClause, java.util.function.Supplier<T> resultFactory,
            TriConsumer<MemorySegment, Arena, T> rowProcessor) {
        return queryWMI(WbemcliFFM.DEFAULT_NAMESPACE, wmiClassName, whereClause, resultFactory, rowProcessor);
    }

    /**
     * Executes a WMI query, processing each result row with a custom processor.
     *
     * @param <T>           the type of result object
     * @param namespace     the WMI namespace
     * @param wmiClassName  the WMI class name
     * @param whereClause   optional WHERE clause (null for none)
     * @param resultFactory factory to create result objects per row
     * @param rowProcessor  processor to populate result object from WMI row
     * @return list of result objects, empty list if query fails
     */
    public <T> List<T> queryWMI(String namespace, String wmiClassName, String whereClause,
            java.util.function.Supplier<T> resultFactory, TriConsumer<MemorySegment, Arena, T> rowProcessor) {
        if (failedWmiClassNames.contains(wmiClassName)) {
            return new ArrayList<>();
        }
        List<T> results = new ArrayList<>();
        boolean comInit = false;
        try (Arena arena = Arena.ofConfined()) {
            comInit = initCOM();
            if (!comInit) {
                return results;
            }
            Optional<MemorySegment> pLocatorOpt = IWbemLocatorFFM.create(arena);
            if (pLocatorOpt.isEmpty()) {
                return results;
            }
            MemorySegment pLocator = pLocatorOpt.get();
            try {
                Optional<MemorySegment> pServicesOpt = IWbemLocatorFFM.connectServer(pLocator, namespace, arena);
                if (pServicesOpt.isEmpty()) {
                    failedWmiClassNames.add(wmiClassName);
                    return results;
                }
                MemorySegment pServices = pServicesOpt.get();
                try {
                    Ole32FFM.CoSetProxyBlanket(pServices, -1, -1, Ole32FFM.RPC_C_AUTHN_LEVEL_CALL,
                            Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE, Ole32FFM.EOAC_NONE);
                    String query = "SELECT * FROM " + wmiClassName;
                    if (whereClause != null && !whereClause.isEmpty()) {
                        query += " " + whereClause;
                    }
                    Optional<MemorySegment> pEnumOpt = IWbemServicesFFM.execQuery(pServices, query, arena);
                    if (pEnumOpt.isEmpty()) {
                        failedWmiClassNames.add(wmiClassName);
                        return results;
                    }
                    MemorySegment pEnum = pEnumOpt.get();
                    try {
                        while (true) {
                            IEnumWbemClassObjectFFM.NextResult nextResult = IEnumWbemClassObjectFFM.next(pEnum,
                                    wmiTimeout, arena);
                            if (nextResult.isComplete() || !nextResult.hasObject()) {
                                break;
                            }
                            MemorySegment pObject = nextResult.pObject();
                            try {
                                T result = resultFactory.get();
                                rowProcessor.accept(pObject, arena, result);
                                results.add(result);
                            } finally {
                                IUnknownFFM.safeRelease(pObject, arena);
                            }
                        }
                    } finally {
                        IUnknownFFM.safeRelease(pEnum, arena);
                    }
                } finally {
                    IUnknownFFM.safeRelease(pServices, arena);
                }
            } finally {
                IUnknownFFM.safeRelease(pLocator, arena);
            }
        } catch (FfmComException e) {
            failedWmiClassNames.add(wmiClassName);
        } catch (Exception e) {
            LOG.debug("WMI query failed for {}: {}", wmiClassName, e.getMessage());
        } finally {
            if (comInit) {
                unInitCOM();
            }
        }
        return results;
    }

    /**
     * Functional interface for processing WMI rows with three parameters.
     *
     * @param <A> the first parameter type
     * @param <B> the second parameter type
     * @param <C> the third parameter type
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    /**
     * COM Exception handler. Logs at debug level for known-optional classes, otherwise warns.
     *
     * @param query a WmiQuery object
     * @param ex    a FfmComException object
     */
    protected void handleComException(WbemcliUtilFFM.WmiQuery<?> query, FfmComException ex) {
        String msg = "COM exception querying {}, which might not be on your system."
                + " Will not attempt to query it again. Error was {}: {}";
        Object[] args = { query.getWmiClassName(), ex.getHresult(), ex.getMessage() };
        if ("MSAcpi_ThermalZoneTemperature".equals(query.getWmiClassName())) {
            LOG.debug(msg, args);
        } else {
            LOG.warn(msg, args);
        }
    }

    /**
     * Initializes COM library and sets security to impersonate the local user.
     *
     * @return True if COM was initialized and needs to be uninitialized, false otherwise
     */
    public boolean initCOM() {
        int threading = getComThreading();
        boolean comInit = initCOM(threading);
        if (!comInit) {
            int switched = switchComThreadingFrom(threading);
            if (switched != threading) {
                comInit = initCOM(switched);
            }
        }
        if (comInit && !securityInitialized) {
            var hrOpt = Ole32FFM.CoInitializeSecurity(Ole32FFM.RPC_C_AUTHN_LEVEL_DEFAULT,
                    Ole32FFM.RPC_C_IMP_LEVEL_IMPERSONATE, Ole32FFM.EOAC_NONE);
            if (hrOpt.isPresent()) {
                int hr = hrOpt.getAsInt();
                if (Ole32FFM.succeeded(hr) || hr == Ole32FFM.RPC_E_TOO_LATE) {
                    securityInitialized = true;
                } else {
                    Ole32FFM.CoUninitialize();
                    throw new FfmComException("Failed to initialize security.", hr);
                }
            }
        }
        return comInit;
    }

    /**
     * Initializes COM with a specific threading model.
     *
     * @param coInitThreading The threading model
     * @return True if COM was initialized and needs to be uninitialized, false otherwise
     */
    protected boolean initCOM(int coInitThreading) {
        var hrOpt = Ole32FFM.CoInitializeEx(coInitThreading);
        if (hrOpt.isEmpty()) {
            return false;
        }
        int hr = hrOpt.getAsInt();
        return switch (hr) {
            case Ole32FFM.S_OK, Ole32FFM.S_FALSE -> true;
            case Ole32FFM.RPC_E_CHANGED_MODE -> false;
            default -> throw new FfmComException("Failed to initialize COM library.", hr);
        };
    }

    /**
     * UnInitializes COM library.
     */
    public void unInitCOM() {
        Ole32FFM.CoUninitialize();
    }

    /**
     * Returns the current threading model for COM initialization.
     *
     * @return The current threading model
     */
    public synchronized int getComThreading() {
        return comThreading;
    }

    /**
     * Switches the current threading model for COM initialization.
     *
     * @return The new threading model after switching
     */
    public synchronized int switchComThreading() {
        if (comThreading == Ole32FFM.COINIT_APARTMENTTHREADED) {
            comThreading = Ole32FFM.COINIT_MULTITHREADED;
        } else {
            comThreading = Ole32FFM.COINIT_APARTMENTTHREADED;
        }
        return comThreading;
    }

    /**
     * Switches the current threading model only if it still matches the expected value.
     *
     * @param expected the threading model observed before the failed initCOM attempt
     * @return the new threading model if switched, or the current value if already switched by another thread
     */
    public synchronized int switchComThreadingFrom(int expected) {
        if (comThreading != expected) {
            return comThreading;
        }
        return switchComThreading();
    }

    /**
     * Security only needs to be initialized once.
     *
     * @return Returns the securityInitialized.
     */
    public boolean isSecurityInitialized() {
        return securityInitialized;
    }

    /**
     * Gets the current WMI timeout.
     *
     * @return Returns the current value of wmiTimeout.
     */
    public int getWmiTimeout() {
        return wmiTimeout;
    }

    /**
     * Sets the WMI timeout.
     *
     * @param wmiTimeout The wmiTimeout to set, in milliseconds. To disable timeouts, set timeout as -1 (infinite).
     */
    public void setWmiTimeout(int wmiTimeout) {
        this.wmiTimeout = wmiTimeout;
    }
}
