/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util.platform.windows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLong; // NOSONAR
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Ole32;
import oshi.jna.platform.windows.COM.EnumWbemClassObject;
import oshi.jna.platform.windows.COM.WbemClassObject;
import oshi.jna.platform.windows.COM.WbemLocator;
import oshi.jna.platform.windows.COM.WbemServices;

/**
 * Utility class providing access to Windows Management Interface (WMI) via COM.
 *
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WmiUtil.class);

    /**
     * Private instance to generate the WmiQuery class.
     */
    private static final WmiUtil INSTANCE = new WmiUtil();

    /**
     * The default namespace for most WMI queries.
     */
    public static final String DEFAULT_NAMESPACE = "ROOT\\CIMV2";

    // Not a built in manespace, failed connections are normal and don't need
    // error logging
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

    // Constants for WMI used often
    private static final BSTR WQL = new BSTR("WQL");
    private static final NativeLong ZERO = new NativeLong(0L);
    private static final NativeLong ONE = new NativeLong(1L);
    private static final NativeLong ASYNCH_FORWARD_FLAGS = new NativeLong(
            EnumWbemClassObject.WBEM_FLAG_FORWARD_ONLY | EnumWbemClassObject.WBEM_FLAG_RETURN_IMMEDIATELY);

    // COM may already have been initialized outside this class. Keep this
    // boolean as a flag whether we need to un-initialize it
    private static boolean comInitialized = false;
    // Security only needs to be initialized once
    private static boolean securityInitialized = false;

    // Cache open WMI namespace connections
    private static Map<String, WmiConnection> connectionCache = new HashMap<>();
    private static long nextCacheClear = System.currentTimeMillis() + WmiConnection.STALE_CONNECTION;
    private static Set<String> hasNamespaceCache = new HashSet<>();
    private static Set<String> hasNotNamespaceCache = new HashSet<>();

    private enum NamespaceProperty {
        NAME;
    }

    private static final WmiQuery<NamespaceProperty> NAMESPACE_QUERY = createQuery("ROOT", "__NAMESPACE",
            NamespaceProperty.class);

    /**
     * Helper class wrapping information required for a WMI query.
     */
    public class WmiQuery<T extends Enum<T>> {
        private String nameSpace;
        private String wmiClassName;
        private Class<T> propertyEnum;

        /**
         * Instantiate a WmiQuery.
         * 
         * @param nameSpace
         *            The WMI namespace to use.
         * @param wmiClassName
         *            The WMI class to use. Optionally include a WQL WHERE
         *            clause with filters results to properties matching the
         *            input.
         * @param propertyEnum
         *            An enum implementing WmiProperty for type mapping.
         */
        public WmiQuery(String nameSpace, String wmiClassName, Class<T> propertyEnum) {
            super();
            this.nameSpace = nameSpace;
            this.wmiClassName = wmiClassName;
            this.propertyEnum = propertyEnum;
        }

        /**
         * @return The enum containing the properties
         */
        public Class<T> getPropertyEnum() {
            return propertyEnum;
        }

        /**
         * @return The namespace
         */
        public String getNameSpace() {
            return nameSpace;
        }

        /**
         * @param nameSpace
         *            The namespace to set
         */
        public void setNameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
        }

        /**
         * @return The class name
         */
        public String getWmiClassName() {
            return wmiClassName;
        }

        /**
         * @param wmiClassName
         *            The classname to set
         */
        public void setWmiClassName(String wmiClassName) {
            this.wmiClassName = wmiClassName;
        }
    }

    /**
     * Helper class wrapping an EnumMap containing the results of a query.
     */
    public class WmiResult<T extends Enum<T>> {
        private Map<T, List<Object>> propertyMap;
        private Map<T, Integer> vtTypeMap;
        private int resultCount = 0;

        /**
         * @param propertyEnum
         *            The enum associated with this map
         */
        public WmiResult(Class<T> propertyEnum) {
            propertyMap = new EnumMap<>(propertyEnum);
            vtTypeMap = new EnumMap<>(propertyEnum);
            for (T type : propertyEnum.getEnumConstants()) {
                propertyMap.put(type, new ArrayList<>());
                vtTypeMap.put(type, Variant.VT_NULL);
            }
        }

        /**
         * Gets a String value from the WmiResult. This is the return type when
         * the WMI result is mapped to a BSTR, including results of UINT64 and
         * DATETIME type which must be further parsed by the user.
         * 
         * @param property
         *            The property (column) to fetch
         * @param index
         *            The index (row) to fetch
         * @return The String containing the specified value.
         */
        public String getString(T property, int index) {
            Object o = this.propertyMap.get(property).get(index);
            if (o == null) {
                return "unknown";
            } else if (vtTypeMap.get(property).equals(Variant.VT_BSTR)) {
                return (String) o;
            }
            throw new IllegalArgumentException(
                    property.name() + " is not a String type. Type is: " + vtTypeMap.get(property));
        }

        /**
         * Gets an Integer value from the WmiResult. This is the return type
         * when the WMI result is mapped to a VT_I4 (4-byte integer) value,
         * including results of UINT32, UINT16, and BOOLEAN. If an unsigned
         * result is desired, it may require further processing by the user.
         * 
         * @param property
         *            The property (column) to fetch
         * @param index
         *            The index (row) to fetch
         * @return The Integer containing the specified value.
         */
        public Integer getInteger(T property, int index) {
            Object o = this.propertyMap.get(property).get(index);
            if (o == null) {
                return 0;
            } else if (vtTypeMap.get(property).equals(Variant.VT_I4)) {
                return (Integer) o;
            }
            throw new IllegalArgumentException(
                    property.name() + " is not an Integer type. Type is: " + vtTypeMap.get(property));
        }

        /**
         * Gets a Float value from the WmiResult. This is the return type when
         * the WMI result is mapped to a VT_R4 (4-byte real) value, including
         * results of FLOAT.
         * 
         * @param property
         *            The property (column) to fetch
         * @param index
         *            The index (row) to fetch
         * @return The Float containing the specified value.
         */
        public Float getFloat(T property, int index) {
            Object o = this.propertyMap.get(property).get(index);
            if (o == null) {
                return 0f;
            } else if (vtTypeMap.get(property).equals(Variant.VT_R4)) {
                return (Float) o;
            }
            throw new IllegalArgumentException(
                    property.name() + " is not a Float type. Type is: " + vtTypeMap.get(property));
        }

        /**
         * Adds a value to the WmiResult at the next index for that property
         * 
         * @param vtType
         *            The Variant type of this object
         * @param property
         *            The property (column) to store
         * @param o
         *            The object to store
         */
        private void add(int vtType, T property, Object o) {
            this.propertyMap.get(property).add(o);
            if (vtType != Variant.VT_NULL && this.vtTypeMap.get(property).equals(Variant.VT_NULL)) {
                this.vtTypeMap.put(property, vtType);
            }
        }

        /**
         * @return The number of results in each mapped list
         */
        public int getResultCount() {
            return this.resultCount;
        }

        /**
         * Increment the result count by one.
         */
        public void incrementResultCount() {
            this.resultCount++;
        }
    }

    /**
     * Helper class for WMI connection caching
     */
    private class WmiConnection {
        // Connection invalid after this long
        public static final long STALE_CONNECTION = 300_000L;

        private long staleAfter;
        private WbemServices svc;

        WmiConnection(PointerByReference pSvc) {
            this.svc = new WbemServices(pSvc.getValue());
            refresh();
        }

        public WbemServices getService() {
            return this.svc;
        }

        public boolean isStale() {
            return System.currentTimeMillis() > staleAfter;
        }

        public void refresh() {
            this.staleAfter = STALE_CONNECTION + System.currentTimeMillis();
        }

        public void close() {
            this.svc.Release();
        }
    }

    /**
     * Private constructor so this class can't be instantiated from the outside.
     * Also initializes COM and sets up hooks to uninit if necessary.
     */
    private WmiUtil() {
        // Initialize COM
        if (!initCOM()) {
            unInitCOM();
        }
        // Set up hook to uninit on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                unInitCOM();
            }
        });
    }

    /**
     * Create a WMI Query
     * 
     * @param <T>
     *            an enum implementing WmiProperty
     * @param nameSpace
     *            The WMI Namespace to use
     * @param wmiClassName
     *            The WMI Class to use. May include a WHERE clause with
     *            filtering conditions.
     * @param propertyEnum
     *            An Enum implementing WmiProperty that contains the properties
     *            and types to query
     * @return A WmiQuery object wrapping the parameters
     */
    public static <T extends Enum<T>> WmiQuery<T> createQuery(String nameSpace, String wmiClassName,
            Class<T> propertyEnum) {
        return INSTANCE.new WmiQuery<>(nameSpace, wmiClassName, propertyEnum);
    }

    /**
     * Create a WMI Query in the default namespace
     * 
     * @param <T>
     *            an enum implementing WmiProperty
     * @param wmiClassName
     *            The WMI Class to use. May include a WHERE clause with
     *            filtering conditions.
     * @param propertyEnum
     *            An Enum implementing WmiProperty that contains the properties
     *            and types to query
     * @return A WmiQuery object wrapping the parameters
     */
    public static <T extends Enum<T>> WmiQuery<T> createQuery(String wmiClassName, Class<T> propertyEnum) {
        return createQuery(DEFAULT_NAMESPACE, wmiClassName, propertyEnum);
    }

    /**
     * Determine if WMI has the requested namespace. Some namespaces only exist
     * on newer versions of Windows.
     *
     * @param namespace
     *            The namespace to test
     * @return true if the namespace exists, false otherwise
     */
    public static boolean hasNamespace(String namespace) {
        if (hasNamespaceCache.contains(namespace)) {
            return true;
        } else if (hasNotNamespaceCache.contains(namespace)) {
            return false;
        }
        WmiResult<NamespaceProperty> namespaces = queryWMI(NAMESPACE_QUERY);
        for (int i = 0; i < namespaces.getResultCount(); i++) {
            if (namespace.equals(namespaces.getString(NamespaceProperty.NAME, i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Query WMI for values, with no timeout.
     * 
     * @param <T>
     *            an enum implementing WmiProperty
     * @param query
     *            A WmiQuery object encapsulating the namespace, class,
     *            properties, and timeout
     * @return a WmiResult object containing the query results, wrapping an
     *         EnumMap
     */
    public static <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {
        try {
            return queryWMI(query, EnumWbemClassObject.WBEM_INFINITE);
        } catch (TimeoutException e) {
            LOG.error("Got a WMI timeout when infinite wait specified. This should never happen.", e);
            return INSTANCE.new WmiResult<>(query.getPropertyEnum());
        }
    }

    /**
     * Query WMI for values, with a specified timeout.
     * 
     * @param <T>
     *            an enum implementing WmiProperty
     * @param query
     *            A WmiQuery object encapsulating the namespace, class,
     *            properties, and timeout
     * @param timeout
     *            Number of milliseconds to wait for results before timing out.
     *            If {@link EnumWbemClassObject#WBEM_INFINITE} (-1), will always
     *            wait for results. If a timeout occurs, throws a
     *            {@link TimeoutException}.
     * @return a WmiResult object containing the query results, wrapping an
     *         EnumMap
     * @throws TimeoutException
     *             if the query times out before completion
     */
    public static <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query, int timeout) throws TimeoutException {
        // Idiot check
        if (query.getPropertyEnum().getEnumConstants().length < 1) {
            throw new IllegalArgumentException("The query's property enum has no values.");
        }

        // Set up empty map
        WmiResult<T> values = INSTANCE.new WmiResult<>(query.getPropertyEnum());

        // Initialize COM if not already done
        if (!comInitialized && !initCOM()) {
            unInitCOM();
            return values;
        }

        // Connect to the server
        WmiConnection conn = connectToNamespace(query.getNameSpace());
        if (conn == null) {
            return values;
        }

        // Send query
        PointerByReference pEnumerator = new PointerByReference();
        if (!selectProperties(conn.getService(), pEnumerator, query)) {
            conn.close();
            return values;
        }
        EnumWbemClassObject enumerator = new EnumWbemClassObject(pEnumerator.getValue());
        try {
            enumerateProperties(values, enumerator, query.getPropertyEnum(), timeout);
        } catch (TimeoutException e) {
            throw new TimeoutException(e.getMessage());
        } finally {
            // Cleanup
            enumerator.Release();
        }
        return values;
    }

    /**
     * Below methods ported from: Getting WMI Data from Local Computer
     * https://docs.microsoft.com/en-us/windows/desktop/WmiSdk/example--getting-
     * wmi-data-from-the-local-computer
     *
     * Steps 1 - 7 in the comments correspond to the above link. Steps 1 through
     * 5 contain all the steps required to set up and connect to WMI, and steps
     * 6 and 7 are where data is queried and received.
     */
    /**
     * Initializes COM library and sets security to impersonate the local user
     *
     * @return true if COM successfully initialized
     */
    private static boolean initCOM() {
        // Step 1: --------------------------------------------------
        // Initialize COM. ------------------------------------------
        HRESULT hres = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
        if (COMUtils.FAILED(hres)) {
            if (hres.intValue() == WinError.RPC_E_CHANGED_MODE) {
                // Com already initialized, ignore error
                LOG.debug("COM already initialized.");
                securityInitialized = true;
                return true;
            }
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("Failed to initialize COM library. Error code = 0x%08x", hres.intValue()));
            }
            return false;
        }
        comInitialized = true;
        if (securityInitialized) {
            // Only run CoInitializeSecuirty once
            return true;
        }
        // Step 2: --------------------------------------------------
        // Set general COM security levels --------------------------
        hres = Ole32.INSTANCE.CoInitializeSecurity(null, new NativeLong(-1), null, null,
                Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
        if (COMUtils.FAILED(hres) && hres.intValue() != WinError.RPC_E_TOO_LATE) {
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("Failed to initialize security. Error code = 0x%08x", hres.intValue()));
            }
            Ole32.INSTANCE.CoUninitialize();
            return false;
        }
        securityInitialized = true;
        return true;
    }

    /**
     * Find an existing open connection to a namespace if one exists, otherwise
     * set up a new one
     * 
     * @param namespace
     *            The namespace to connect to
     * @return The new or cached connection if successful; null if connection
     *         failed
     */
    private static WmiConnection connectToNamespace(String namespace) {
        // Every once in a while clear any other stale connections
        if (System.currentTimeMillis() > nextCacheClear) {
            closeStaleConnections();
            nextCacheClear = System.currentTimeMillis() + WmiConnection.STALE_CONNECTION;
        }
        // Check if connection already open
        if (connectionCache.containsKey(namespace)) {
            WmiConnection conn = connectionCache.get(namespace);
            if (conn.isStale()) {
                // Connection expired. Close it.
                conn.close();
                connectionCache.remove(namespace);
            } else {
                return conn;
            }
        }
        // Connect to the server
        PointerByReference pSvc = new PointerByReference();
        if (!connectServer(namespace, pSvc)) {
            return null;
        }
        WmiConnection conn = INSTANCE.new WmiConnection(pSvc);
        // Add to cache
        connectionCache.put(namespace, conn);
        return conn;
    }

    /**
     * Obtains a locator to the WMI server and connects to the specified
     * namespace
     *
     * @param namespace
     *            The namespace to connect to
     * @param pSvc
     *            A pointer to receive an indirect to the WMI service
     * @return true if successful; pSvc will contain an indirect pointer to the
     *         WMI service for future IWbemServices calls
     */
    private static boolean connectServer(String namespace, PointerByReference pSvc) {
        // Step 3: ---------------------------------------------------
        // Obtain the initial locator to WMI -------------------------
        WbemLocator loc = WbemLocator.create();
        if (loc == null) {
            return false;
        }
        // Step 4: -----------------------------------------------------
        // Connect to WMI through the IWbemLocator::ConnectServer method
        // Connect to the namespace with the current user and obtain pointer
        // pSvc to make IWbemServices calls.
        HRESULT hres = loc.ConnectServer(new BSTR(namespace), null, null, null, null, null, null, pSvc);
        if (COMUtils.FAILED(hres)) {
            // Don't error on OpenHardwareMonitor
            if (!OHM_NAMESPACE.equals(namespace) && LOG.isErrorEnabled()) {
                LOG.error(String.format("Could not connect to namespace %s. Error code = 0x%08x", namespace,
                        hres.intValue()));
            }
            loc.Release();
            unInitCOM();
            return false;
        }
        LOG.debug("Connected to {} WMI namespace", namespace);
        loc.Release();

        // Step 5: --------------------------------------------------
        // Set security levels on the proxy -------------------------
        hres = Ole32.INSTANCE.CoSetProxyBlanket(pSvc.getValue(), Ole32.RPC_C_AUTHN_WINNT, Ole32.RPC_C_AUTHZ_NONE, null,
                Ole32.RPC_C_AUTHN_LEVEL_CALL, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE);
        if (COMUtils.FAILED(hres)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("Could not set proxy blanket. Error code = 0x%08x", hres.intValue()));
            }
            new WbemServices(pSvc.getValue()).Release();
            unInitCOM();
            return false;
        }
        LOG.debug("Proxy blanket set.");
        return true;
    }

    /**
     * Selects properties from WMI. Returns immediately, even while results are
     * being retrieved; results may begun to be enumerated in the forward
     * direction only.
     *
     * @param svc
     *            A WbemServices object to make the calls
     * @param pEnumerator
     *            An enumerator to receive the results of the query
     * @param query
     *            A WmiQuery object encapsulating the details of the query
     * @return True if successful. The enumerator will allow enumeration of
     *         results of the query
     */
    private static <T extends Enum<T>> boolean selectProperties(WbemServices svc, PointerByReference pEnumerator,
            WmiQuery<T> query) {
        // Step 6: --------------------------------------------------
        // Use the IWbemServices pointer to make requests of WMI ----
        T[] props = query.getPropertyEnum().getEnumConstants();
        StringBuilder sb = new StringBuilder("SELECT ");
        // We earlier checked for at least one enum constant
        sb.append(props[0].name());
        for (int i = 1; i < props.length; i++) {
            sb.append(',').append(props[i].name());
        }
        sb.append(" FROM ").append(query.getWmiClassName());
        LOG.debug("Query: {}", sb);
        // Send the query. The flags allow us to return immediately and begin
        // enumerating in the forward direction as results come in.
        HRESULT hres = svc.ExecQuery(WQL, new BSTR(sb.toString().replaceAll("\\\\", "\\\\\\\\")), ASYNCH_FORWARD_FLAGS,
                null, pEnumerator);
        if (COMUtils.FAILED(hres)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("Query '%s' failed. Error code = 0x%08x", sb, hres.intValue()));
            }
            svc.Release();
            unInitCOM();
            return false;
        }
        LOG.debug("Query succeeded.");
        return true;
    }

    /**
     * Enumerate the results of a WMI query. This method is called while results
     * are still being retrieved and may iterate in the forward direction only.
     * 
     * @param values
     *            A WmiResult object encapsulating an EnumMap which will hold
     *            the results.
     * @param enumerator
     *            The enumerator with the results
     * @param propertyEnum
     *            The enum containing the properties to enumerate, which are the
     *            keys to the WmiResult map
     * @param timeout
     *            Number of milliseconds to wait for results before timing out.
     *            If {@link EnumWbemClassObject#WBEM_INFINITE} (-1), will always
     *            wait for results.
     * @throws TimeoutException
     *             if the query times out before completion
     */
    private static <T extends Enum<T>> void enumerateProperties(WmiResult<T> values, EnumWbemClassObject enumerator,
            Class<T> propertyEnum, int timeout) throws TimeoutException {
        // Step 7: -------------------------------------------------
        // Get the data from the query in step 6 -------------------
        PointerByReference pclsObj = new PointerByReference();
        LongByReference uReturn = new LongByReference(0L);
        Map<T, BSTR> bstrMap = new HashMap<>();
        for (T property : propertyEnum.getEnumConstants()) {
            bstrMap.put(property, new BSTR(property.name()));
        }
        while (enumerator.getPointer() != Pointer.NULL) {
            // Enumerator will be released by calling method so no need to
            // release it here.
            HRESULT hres = enumerator.Next(new NativeLong(timeout), ONE, pclsObj, uReturn);
            // Warn user of timeout and abort. This should never happen if
            // timeout is set to infinite.
            if (hres.intValue() == EnumWbemClassObject.WBEM_S_TIMEDOUT) {
                throw new TimeoutException("No results after " + timeout + " ms.");
            }
            // Requested 1; if 0 objects returned, we're done
            if (COMUtils.FAILED(hres) || 0L == uReturn.getValue()
                    || hres.intValue() == EnumWbemClassObject.WBEM_S_NO_MORE_DATA) {
                LOG.debug("Returned {} results.", values.getResultCount());
                break;
            }

            VARIANT.ByReference pVal = new VARIANT.ByReference();

            // Get the value of the properties
            WbemClassObject clsObj = new WbemClassObject(pclsObj.getValue());
            for (T property : propertyEnum.getEnumConstants()) {
                clsObj.Get(bstrMap.get(property), ZERO, pVal, null, null);
                int type = (pVal.getValue() == null ? Variant.VT_NULL : pVal.getVarType()).intValue();
                switch (type) {
                case Variant.VT_BSTR:
                    values.add(type, property, pVal.stringValue());
                    break;
                case Variant.VT_I4:
                    values.add(type, property, pVal.intValue());
                    break;
                case Variant.VT_R4:
                    values.add(type, property, pVal.floatValue());
                    break;
                case Variant.VT_NULL:
                    values.add(type, property, null);
                    break;
                default:
                    throw new IllegalArgumentException("Unimplemented Variant type: " + type);
                }
                OleAuto.INSTANCE.VariantClear(pVal);
            }
            clsObj.Release();

            values.incrementResultCount();
        }
    }

    /**
     * UnInitializes COM library if it was initialized by the {@link #initCOM()}
     * method. Otherwise, does nothing.
     */
    private static void unInitCOM() {
        for (Entry<String, WmiConnection> entry : connectionCache.entrySet()) {
            entry.getValue().close();
        }
        connectionCache.clear();
        if (comInitialized) {
            Ole32.INSTANCE.CoUninitialize();
            comInitialized = false;
        }
    }

    /**
     * Closes WMI connections that haven't been used recently, freeing up
     * resources.
     */
    private static void closeStaleConnections() {
        for (Iterator<Map.Entry<String, WmiConnection>> iter = connectionCache.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<String, WmiConnection> entry = iter.next();
            if (entry.getValue().isStale()) {
                entry.getValue().close();
                iter.remove();
            }
        }
    }
}
