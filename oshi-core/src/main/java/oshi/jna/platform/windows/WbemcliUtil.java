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
package oshi.jna.platform.windows;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Wbemcli.IEnumWbemClassObject;
import oshi.jna.platform.windows.Wbemcli.IWbemClassObject;
import oshi.jna.platform.windows.Wbemcli.IWbemLocator;
import oshi.jna.platform.windows.Wbemcli.IWbemServices;

/**
 * Utility class providing access to Windows Management Interface (WMI) via COM.
 */
public class WbemcliUtil {
    /**
     * Instance to generate the WmiQuery class.
     */
    public static final WbemcliUtil INSTANCE = new WbemcliUtil();

    /**
     * The default namespace for most WMI queries.
     */
    public static final String DEFAULT_NAMESPACE = "ROOT\\CIMV2";

    /**
     * Enum containing the property used for WMI Namespace query.
     */
    private enum NamespaceProperty {
        NAME;
    }

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
         *            An enum for type mapping.
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
        private Map<T, Integer> cimTypeMap;
        private int resultCount = 0;

        /**
         * @param propertyEnum
         *            The enum associated with this map
         */
        public WmiResult(Class<T> propertyEnum) {
            propertyMap = new EnumMap<>(propertyEnum);
            vtTypeMap = new EnumMap<>(propertyEnum);
            cimTypeMap = new EnumMap<>(propertyEnum);
            for (T type : propertyEnum.getEnumConstants()) {
                propertyMap.put(type, new ArrayList<>());
                vtTypeMap.put(type, Variant.VT_NULL);
                cimTypeMap.put(type, Wbemcli.CIM_EMPTY);
            }
        }

        /**
         * Gets a value from the WmiResult, which may be null. User must check
         * for null and cast the result. Types correlate to the CIM Type of the
         * enumerated WMI property and will be consistent for a given property,
         * and may be further validated by the user using {@link #getVtType} or
         * the Class of the returned Object.
         * 
         * @param property
         *            The property (column) to fetch
         * @param index
         *            The index (row) to fetch
         * @return The Object containing the specified value, which may be null
         */
        public Object getValue(T property, int index) {
            return this.propertyMap.get(property).get(index);
        }

        /**
         * Gets the Variant type from the WmiResult. The integer value is
         * defined as a VT_* constant in the
         * {@link com.sun.jna.platform.win32.Variant} interface.
         * 
         * @param property
         *            The property (column) whose type to fetch
         * @return An integer representing the Variant type, if any of the
         *         values are non-null. If all values in this column are null,
         *         returns VT_NULL.
         */
        public int getVtType(T property) {
            return this.vtTypeMap.get(property);
        }

        /**
         * Gets the CIM type from the WmiResult. The integer value is defined as
         * a CIM_* constant in the {@link Wbemcli} interface.
         * 
         * @param property
         *            The property (column) whose type to fetch
         * @return An integer representing the CIM type
         */
        public int getCIMType(T property) {
            return this.cimTypeMap.get(property);
        }

        /**
         * Adds a value to the WmiResult at the next index for that property
         * 
         * @param vtType
         *            The Variant type of this object
         * @param cimType
         *            The CIM type of this property
         * @param property
         *            The property (column) to store
         * @param o
         *            The object to store
         */
        private void add(int vtType, int cimType, T property, Object o) {
            this.propertyMap.get(property).add(o);
            if (vtType != Variant.VT_NULL && this.vtTypeMap.get(property).equals(Variant.VT_NULL)) {
                this.vtTypeMap.put(property, vtType);
            }
            if (this.cimTypeMap.get(property).equals(Wbemcli.CIM_EMPTY)) {
                this.cimTypeMap.put(property, cimType);
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
        private void incrementResultCount() {
            this.resultCount++;
        }
    }

    /**
     * Create a WMI Query
     * 
     * @param <T>
     *            an enum
     * @param nameSpace
     *            The WMI Namespace to use
     * @param wmiClassName
     *            The WMI Class to use. May include a WHERE clause with
     *            filtering conditions.
     * @param propertyEnum
     *            An Enum that contains the properties to query
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
     *            an enum
     * @param wmiClassName
     *            The WMI Class to use. May include a WHERE clause with
     *            filtering conditions.
     * @param propertyEnum
     *            An Enum that contains the properties to query
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
        // Strip off leading ROOT\ for valid match
        String ns = namespace;
        if (namespace.toUpperCase().startsWith("ROOT\\")) {
            ns = namespace.substring(5);
        }
        // Test
        WmiQuery<NamespaceProperty> namespaceQuery = createQuery("ROOT", "__NAMESPACE", NamespaceProperty.class);
        WmiResult<NamespaceProperty> namespaces = queryWMI(namespaceQuery);
        for (int i = 0; i < namespaces.getResultCount(); i++) {
            if (ns.equalsIgnoreCase((String) namespaces.getValue(NamespaceProperty.NAME, i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Query WMI for values, with no timeout.
     * 
     * @param <T>
     *            an enum
     * @param query
     *            A WmiQuery object encapsulating the namespace, class, and
     *            properties
     * @return a WmiResult object containing the query results, wrapping an
     *         EnumMap
     */
    public static <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {
        try {
            return queryWMI(query, Wbemcli.WBEM_INFINITE);
        } catch (TimeoutException e) {
            throw new COMException("Got a WMI timeout when infinite wait was specified. This should never happen.");
        }
    }

    /**
     * Query WMI for values, with a specified timeout.
     * 
     * @param <T>
     *            an enum
     * @param query
     *            A WmiQuery object encapsulating the namespace, class, and
     *            properties
     * @param timeout
     *            Number of milliseconds to wait for results before timing out.
     *            If {@link IEnumWbemClassObject#WBEM_INFINITE} (-1), will
     *            always wait for results. If a timeout occurs, throws a
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

        // Connect to the server
        IWbemServices svc = connectServer(query.getNameSpace());

        // Send query
        try {
            IEnumWbemClassObject enumerator = selectProperties(svc, query);

            try {
                return enumerateProperties(enumerator, query.getPropertyEnum(), timeout);
            } finally {
                // Cleanup
                enumerator.Release();
            }
        } finally {
            // Cleanup
            svc.Release();
        }
    }

    /*
     * Below methods ported from: Getting WMI Data from Local Computer
     * https://docs.microsoft.com/en-us/windows/desktop/WmiSdk/example--getting-
     * wmi-data-from-the-local-computer
     *
     * Steps in the comments correspond to the above link. Steps 1 - 2 are the
     * responsibility of the user. Steps 3 - 5 contain all the steps required to
     * set up and connect to WMI, and steps 6 and 7 are where data is queried
     * and received.
     */

    /**
     * Obtains a locator to the WMI server and connects to the specified
     * namespace
     *
     * @param namespace
     *            The namespace to connect to
     * @return A service representing the connected namespace, which can be
     *         queried. This service may be re-used for multiple queries and
     *         should be released by the user
     */
    public static IWbemServices connectServer(String namespace) {
        PointerByReference pSvc = new PointerByReference();
        // Step 3: ---------------------------------------------------
        // Obtain the initial locator to WMI -------------------------
        IWbemLocator loc = IWbemLocator.create();
        if (loc == null) {
            throw new COMException("Could not create IWbemLocator.");
        }

        // Step 4: -----------------------------------------------------
        // Connect to WMI through the IWbemLocator::ConnectServer method
        // Connect to the namespace with the current user and obtain pointer
        // pSvc to make IWbemServices calls.
        BSTR namespaceStr = OleAuto.INSTANCE.SysAllocString(namespace);
        HRESULT hres = loc.ConnectServer(namespaceStr, null, null, null, 0, null, null, pSvc);
        OleAuto.INSTANCE.SysFreeString(namespaceStr);
        // Release the locator. If successful, pSvc contains connection
        // information
        loc.Release();
        if (COMUtils.FAILED(hres)) {
            throw new COMException(
                    String.format("Could not connect to namespace %s. Error %08x", namespace, hres.intValue()));
        }

        // Step 5: --------------------------------------------------
        // Set security levels on the proxy -------------------------
        hres = Ole32.INSTANCE.CoSetProxyBlanket(pSvc.getValue(), Ole32.RPC_C_AUTHN_WINNT, Ole32.RPC_C_AUTHZ_NONE, null,
                Ole32.RPC_C_AUTHN_LEVEL_CALL, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE);
        if (COMUtils.FAILED(hres)) {
            new IWbemServices(pSvc.getValue()).Release();
            throw new COMException(String.format("Could not set proxy blanket. Error %08x", hres.intValue()));
        }
        return new IWbemServices(pSvc.getValue());
    }

    /**
     * Selects properties from WMI. Returns immediately (asynchronously), even
     * while results are being retrieved; results may begun to be enumerated in
     * the forward direction only.
     *
     * @param <T>
     *            The enum containing properties to query
     * @param svc
     *            A WbemServices object to make the calls
     * @param query
     *            A WmiQuery object encapsulating the details of the query
     * @return An enumerator to receive the results of the query
     */
    public static <T extends Enum<T>> IEnumWbemClassObject selectProperties(IWbemServices svc, WmiQuery<T> query) {
        PointerByReference pEnumerator = new PointerByReference();
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
        // Send the query. The flags allow us to return immediately and begin
        // enumerating in the forward direction as results come in.
        BSTR queryStr = OleAuto.INSTANCE.SysAllocString(sb.toString().replaceAll("\\\\", "\\\\\\\\"));
        BSTR wql = OleAuto.INSTANCE.SysAllocString("WQL");
        HRESULT hres = svc.ExecQuery(wql, queryStr,
                Wbemcli.WBEM_FLAG_FORWARD_ONLY | Wbemcli.WBEM_FLAG_RETURN_IMMEDIATELY, null, pEnumerator);
        OleAuto.INSTANCE.SysFreeString(queryStr);
        OleAuto.INSTANCE.SysFreeString(wql);
        if (COMUtils.FAILED(hres)) {
            throw new COMException(String.format("Query '%s' failed. Error %08x", sb.toString(), hres.intValue()));
        }
        return new IEnumWbemClassObject(pEnumerator.getValue());
    }

    /**
     * Enumerate the results of a WMI query. This method is called while results
     * are still being retrieved and may iterate in the forward direction only.
     * 
     * @param <T>
     *            The enum containing properties to query
     * @param enumerator
     *            The enumerator with the results
     * @param propertyEnum
     *            The enum containing the properties to enumerate, which are the
     *            keys to the WmiResult map
     * @param timeout
     *            Number of milliseconds to wait for results before timing out.
     *            If {@link IEnumWbemClassObject#WBEM_INFINITE} (-1), will
     *            always wait for results.
     * @return A WmiResult object encapsulating an EnumMap which will hold the
     *         results.
     * @throws TimeoutException
     *             if the query times out before completion
     */
    public static <T extends Enum<T>> WmiResult<T> enumerateProperties(IEnumWbemClassObject enumerator,
            Class<T> propertyEnum, int timeout) throws TimeoutException {
        WmiResult<T> values = INSTANCE.new WmiResult<>(propertyEnum);
        // Step 7: -------------------------------------------------
        // Get the data from the query in step 6 -------------------
        PointerByReference pclsObj = new PointerByReference();
        IntByReference uReturn = new IntByReference(0);
        Map<T, WString> wstrMap = new HashMap<>();
        HRESULT hres = null;
        for (T property : propertyEnum.getEnumConstants()) {
            wstrMap.put(property, new WString(property.name()));
        }
        while (enumerator.getPointer() != Pointer.NULL) {
            // Enumerator will be released by calling method so no need to
            // release it here.
            hres = enumerator.Next(timeout, 1, pclsObj, uReturn);
            // Enumeration complete or no more data; we're done, exit the loop
            if (hres.intValue() == Wbemcli.WBEM_S_FALSE || hres.intValue() == Wbemcli.WBEM_S_NO_MORE_DATA) {
                break;
            }
            // Throw exception to notify user of timeout
            if (hres.intValue() == Wbemcli.WBEM_S_TIMEDOUT) {
                throw new TimeoutException("No results after " + timeout + " ms.");
            }
            // Other exceptions here.
            if (COMUtils.FAILED(hres)) {
                throw new COMException(String.format("Failed to enumerate results. Error %08x", hres.intValue()));
            }

            VARIANT.ByReference pVal = new VARIANT.ByReference();
            IntByReference pType = new IntByReference();

            // Get the value of the properties
            IWbemClassObject clsObj = new IWbemClassObject(pclsObj.getValue());
            for (T property : propertyEnum.getEnumConstants()) {
                clsObj.Get(wstrMap.get(property), 0, pVal, pType, null);
                int vtType = (pVal.getValue() == null ? Variant.VT_NULL : pVal.getVarType()).intValue();
                int cimType = pType.getValue();
                switch (vtType) {
                case Variant.VT_BSTR:
                    values.add(vtType, cimType, property, pVal.stringValue());
                    break;
                case Variant.VT_I4:
                    values.add(vtType, cimType, property, pVal.intValue());
                    break;
                case Variant.VT_UI1:
                    values.add(vtType, cimType, property, pVal.byteValue());
                    break;
                case Variant.VT_I2:
                    values.add(vtType, cimType, property, pVal.shortValue());
                    break;
                case Variant.VT_BOOL:
                    values.add(vtType, cimType, property, pVal.booleanValue());
                    break;
                case Variant.VT_R4:
                    values.add(vtType, cimType, property, pVal.floatValue());
                    break;
                case Variant.VT_R8:
                    values.add(vtType, cimType, property, pVal.doubleValue());
                    break;
                // Null or Unimplemented type. User must cast
                default:
                    values.add(vtType, cimType, property, pVal.getValue());
                }
                OleAuto.INSTANCE.VariantClear(pVal);
            }
            clsObj.Release();

            values.incrementResultCount();
        }
        return values;
    }
}
