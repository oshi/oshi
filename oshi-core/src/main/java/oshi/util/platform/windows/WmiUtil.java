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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLong; // NOSONAR
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.OleAuto;
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
import oshi.util.ParseUtil;

/**
 * Provides access to WMI queries via COM
 *
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WmiUtil.class);

    public static final String DEFAULT_NAMESPACE = "ROOT\\CIMV2";

    private static boolean comInitialized = false;
    private static boolean securityInitialized = false;

    /**
     * Private instance to generate the WmiQuery class
     */
    private static final WmiUtil INSTANCE = new WmiUtil();
    /**
     * Private constructor so this class can't be instantiated from the outside
     */
    private WmiUtil() {
    }

    /**
     * Enum for the value type of WMI queries for proper parsing from the
     * returned VARIANT, using the same name as the WMI documentation. Note WMI
     * UINT64s return from WMI as Strings. With the UINT64 mapping, OSHI will
     * parse the object to a Long. If true unsigned longs are required, users
     * may prefer to use the String type to parse to a BigInteger.
     */
    public enum ValueType {
        // Properties
        STRING, UINT32, FLOAT, DATETIME, BOOLEAN, UINT64, UINT16
    }

    /**
     * Interface contract for WMI Property Enums.
     */
    public interface WmiProperty {
        /**
         * Gets the ValueType.
         * 
         * @return The type of value this property returns
         */
        public ValueType getType();
    }

    /**
     * Helper class wrapping information required for the WMI query.
     */
    /**
     * @author widdisd
     *
     * @param <T>
     */
    public class WmiQuery<T extends Enum<T> & WmiProperty> {
        private String nameSpace;
        private String wmiClassName;
        private Class<T> propertyEnum;
        private int timeout;

        /**
         * Instantiate a WmiQuery.
         * 
         * @param nameSpace
         *            The WMI namespace to use.
         * @param wmiClassName
         *            The WMI class to use. Optionally include a WHERE clause
         *            with filters.
         * @param propertyEnum
         *            An enum implementing WmiProperty for type mapping.
         * @param timeout
         *            Number of milliseconds to wait for results before timing
         *            out. If -1, will always wait for results. If 0, will
         *            always return immediately without waiting.
         */
        public WmiQuery(String nameSpace, String wmiClassName, Class<T> propertyEnum, int timeout) {
            super();
            this.nameSpace = nameSpace;
            this.wmiClassName = wmiClassName;
            this.propertyEnum = propertyEnum;
            this.timeout = timeout;
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

        /**
         * @return the timeout
         */
        public int getTimeout() {
            return timeout;
        }

        /**
         * @param timeout
         *            The timeout to set
         */
        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }
    }

    /**
     * Helper class wrapping an EnumMap containing the results of a query.
     */
    public class WmiResult<T extends Enum<T> & WmiProperty> {
        private Map<T, List<Object>> propertyMap;
        private int resultCount = 0;

        /**
         * @param propertyEnum
         *            The enum associated with this map
         */
        public WmiResult(Class<T> propertyEnum) {
            propertyMap = new EnumMap<>(propertyEnum);
            for (T type : propertyEnum.getEnumConstants()) {
                propertyMap.put(type, new ArrayList<>());
            }
        }

        /**
         * @param item
         *            An element of the Enum
         * @return the list associated with that enum
         */
        public List<Object> get(final T item) {
            return this.propertyMap.get(item);
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

    enum NamespaceProperty implements WmiProperty {
        NAME(ValueType.STRING);

        private ValueType type;

        NamespaceProperty(ValueType type) {
            this.type = type;
        }

        @Override
        public ValueType getType() {
            return this.type;
        }
    }

    private static final WmiQuery<NamespaceProperty> NAMESPACE_QUERY = createQuery("ROOT",
            "__NAMESPACE", NamespaceProperty.class);

    /**
     * Create a WMI Query
     * 
     * @param nameSpace
     *            The WMI Namespace to use
     * @param wmiClassName
     *            The WMI Class to use. May include a WHERE clause with
     *            filtering conditions.
     * @param propertyEnum
     *            An Enum implementing WmiProperty that contains the properties
     *            and types to query
     * @param timeout
     *            Number of milliseconds to wait for results before timing out.
     *            If -1, will always wait for results. If 0, will always return
     *            immediately without waiting.
     * @return A WmiQuery object wrapping the parameters
     */
    public static <T extends Enum<T> & WmiProperty> WmiQuery<T> createQuery(String nameSpace, String wmiClassName,
            Class<T> propertyEnum, int timeout) {
        return INSTANCE.new WmiQuery<>(nameSpace, wmiClassName, propertyEnum, timeout);
    }

    /**
     * Create a WMI Query with no timeout
     * 
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
    public static <T extends Enum<T> & WmiProperty> WmiQuery<T> createQuery(String nameSpace, String wmiClassName,
            Class<T> propertyEnum) {
        return createQuery(nameSpace, wmiClassName, propertyEnum, EnumWbemClassObject.WBEM_INFINITE);
    }

    /**
     * Create a WMI Query in the default namespace with no timeout
     * 
     * @param wmiClassName
     *            The WMI Class to use. May include a WHERE clause with
     *            filtering conditions.
     * @param propertyEnum
     *            An Enum implementing WmiProperty that contains the properties
     *            and types to query
     * @return A WmiQuery object wrapping the parameters
     */
    public static <T extends Enum<T> & WmiProperty> WmiQuery<T> createQuery(String wmiClassName,
            Class<T> propertyEnum) {
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
        WmiResult<NamespaceProperty> namespaces = queryWMI(NAMESPACE_QUERY);
        for (int i = 0; i < namespaces.getResultCount(); i++) {
            if (namespace.equals((String) namespaces.get(NamespaceProperty.NAME).get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Query WMI for values
     * 
     * @param query
     *            A WmiQuery object encapsulating the namespace, class,
     *            properties, and timeout
     * @return a WmiResult object containing the query results, wrapping an
     *         EnumMap
     */
    public static <T extends Enum<T> & WmiProperty> WmiResult<T> queryWMI(WmiQuery<T> query) {
        // Idiot check
        if (query.getPropertyEnum().getEnumConstants().length < 1) {
            throw new IllegalArgumentException("The query's property enum has no values.");
        }

        // Set up empty map
        WmiResult<T> values = INSTANCE.new WmiResult<>(query.getPropertyEnum());

        // Initialize COM
        if (!initCOM()) {
            unInitCOM();
            return values;
        }

        // Connect to the server
        PointerByReference pSvc = new PointerByReference();
        if (!connectServer(query.getNameSpace(), pSvc)) {
            unInitCOM();
            return values;
        }
        WbemServices svc = new WbemServices(pSvc.getValue());

        // Send query
        PointerByReference pEnumerator = new PointerByReference();
        if (!selectProperties(svc, pEnumerator, query)) {
            svc.Release();
            unInitCOM();
            return values;
        }
        EnumWbemClassObject enumerator = new EnumWbemClassObject(pEnumerator.getValue());
        enumerateProperties(values, enumerator, query.getPropertyEnum(), svc, query.getTimeout());

        // Cleanup
        enumerator.Release();
        svc.Release();
        unInitCOM();
        return values;
    }

    /*
     * Below methods ported from: Getting WMI Data from Local Computer
     * https://msdn.microsoft.com/en-us/library/aa390423(v=VS.85).aspx
     *
     * Steps 1 - 7 in the comments correspond to the above link.
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
            if (!"root\\OpenHardwareMonitor".equals(namespace) && LOG.isErrorEnabled()) {
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
    private static <T extends Enum<T> & WmiProperty> boolean selectProperties(WbemServices svc,
            PointerByReference pEnumerator, WmiQuery<T> query) {
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
        HRESULT hres = svc.ExecQuery(new BSTR("WQL"), new BSTR(sb.toString()),
                new NativeLong(
                        EnumWbemClassObject.WBEM_FLAG_FORWARD_ONLY | EnumWbemClassObject.WBEM_FLAG_RETURN_IMMEDIATELY),
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
     * @param svc
     *            The WbemServices object
     * @param timeout
     *            Number of milliseconds to wait for results before timing out.
     *            If -1, will always wait for results. If 0, will always return
     *            immediately without waiting.
     * @return True if the query completed successfully, false if the query
     *         timed out.
     */
    private static <T extends Enum<T> & WmiProperty> boolean enumerateProperties(WmiResult<T> values,
            EnumWbemClassObject enumerator, Class<T> propertyEnum, WbemServices svc, int timeout) {
        // Step 7: -------------------------------------------------
        // Get the data from the query in step 6 -------------------
        PointerByReference pclsObj = new PointerByReference();
        LongByReference uReturn = new LongByReference(0L);
        boolean success = true;
        while (enumerator.getPointer() != Pointer.NULL) {
            // Enumerator will be released by calling method so no need to
            // release it here.
            HRESULT hres = enumerator.Next(new NativeLong(timeout), new NativeLong(1), pclsObj, uReturn);
            // Warn user of timeout
            if (hres.intValue() == EnumWbemClassObject.WBEM_S_TIMEDOUT) {
                LOG.warn("WMI query timed out. Returned {} results.", values.getResultCount());
                success = false;
                break;
            }
            // Error values are hres < 0
            if (COMUtils.FAILED(hres)) {
                LOG.debug("WMI query failed. Returned {} results.", values.getResultCount());
                break;
            }
            // Requested 1; if 0 objects returned, we're done
            if (0L == uReturn.getValue() || hres.intValue() == EnumWbemClassObject.WBEM_S_NO_MORE_DATA) {
                LOG.debug("Returned {} results.", values.getResultCount());
                break;
            }

            VARIANT.ByReference vtProp = new VARIANT.ByReference();

            // Get the value of the properties
            WbemClassObject clsObj = new WbemClassObject(pclsObj.getValue());
            for (T property : propertyEnum.getEnumConstants()) {
                clsObj.Get(new BSTR(property.name()), new NativeLong(0L), vtProp, null, null);

                switch (property.getType()) {
                case STRING:
                    values.get(property).add(vtProp.getValue() == null ? "unknown" : vtProp.stringValue());
                    break;
                // uint16 == VT_I4, a 32-bit number
                case UINT16:
                    values.get(property).add(vtProp.getValue() == null ? 0L : vtProp.intValue());
                    break;
                // WMI Uint32s will return as longs
                case UINT32:
                    values.get(property).add(vtProp.getValue() == null ? 0L : vtProp.longValue());
                    break;
                // WMI Longs will return as strings so we have the option of
                // calling a string and parsing later, or calling UINT64 and
                // letting this method do the parsing
                case UINT64:
                    values.get(property).add(
                            vtProp.getValue() == null ? 0L : ParseUtil.parseLongOrDefault(vtProp.stringValue(), 0L));
                    break;
                case FLOAT:
                    values.get(property).add(vtProp.getValue() == null ? 0f : vtProp.floatValue());
                    break;
                case DATETIME:
                    // Read a string in format 20160513072950.782000-420 and
                    // parse to a long representing ms since eopch
                    values.get(property)
                            .add(vtProp.getValue() == null ? 0L : ParseUtil.cimDateTimeToMillis(vtProp.stringValue()));
                    break;
                case BOOLEAN:
                    values.get(property).add(vtProp.getValue() == null ? 0L : vtProp.booleanValue());
                    break;
                default:
                    // Should never get here! If you get this exception you've
                    // added something to the ValueType enum without adding a
                    // case for it here. Tsk.
                    throw new IllegalArgumentException("Unimplemented enum type: " + property.getType().toString());
                }
                OleAuto.INSTANCE.VariantClear(vtProp);
            }
            clsObj.Release();

            values.incrementResultCount();
        }
        return success;
    }

    /**
     * UnInitializes COM library if it was initialized by the {@link #initCOM()}
     * method. Otherwise, does nothing.
     */
    private static void unInitCOM() {
        if (comInitialized) {
            Ole32.INSTANCE.CoUninitialize();
            comInitialized = false;
        }
    }
}
