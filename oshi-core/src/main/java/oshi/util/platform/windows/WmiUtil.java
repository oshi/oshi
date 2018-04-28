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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.OleAuto;
import com.sun.jna.platform.win32.Variant.VARIANT;
import com.sun.jna.platform.win32.WTypes.BSTR;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import oshi.jna.platform.windows.Ole32;
import oshi.jna.platform.windows.COM.EnumWbemClassObject;
import oshi.jna.platform.windows.COM.WbemClassObject;
import oshi.jna.platform.windows.COM.WbemLocator;
import oshi.jna.platform.windows.COM.WbemServices;
import oshi.util.FormatUtil;
import oshi.util.ParseUtil;

/**
 * Provides access to WMI queries
 *
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WmiUtil.class);

    public static final String DEFAULT_NAMESPACE = "ROOT\\CIMV2";

    private static boolean comInitialized = false;
    private static boolean securityInitialized = false;

    /**
     * Enum for WMI queries for proper parsing from the returned VARIANT
     */
    public enum ValueType {
        // Properties
        STRING, UINT32, FLOAT, DATETIME, BOOLEAN, UINT64, UINT16,
        // Methods (use "__PATH" for property)
        PROCESS_GETOWNER, PROCESS_GETOWNERSID
    }

    /**
     * For WMI queries requiring array input
     */
    private static final ValueType[] STRING_TYPE = { ValueType.STRING };
    private static final ValueType[] UINT32_TYPE = { ValueType.UINT32 };
    private static final ValueType[] FLOAT_TYPE = { ValueType.FLOAT };

    /**
     * Determine if WMI has the requested namespace. Some namespaces only exist
     * on newer versions of Windows.
     *
     * @param namespace
     *            The namespace to test
     * @return true if the namespace exists, false otherwise
     */
    public static boolean hasNamespace(String namespace) {
        Map<String, List<String>> nsMap = WmiUtil.selectStringsFrom("ROOT", "__NAMESPACE", "Name", null);
        for (String s : nsMap.get("Name")) {
            if (s.equals(namespace)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a single Unsigned Integer value from WMI (as Long)
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param property
     *            The property whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @return A Long containing the value of the requested property
     */
    public static Long selectUint32From(String namespace, String wmiClass, String property, String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, property,
                wmiClass, whereClause, UINT32_TYPE);
        if (result.containsKey(property) && !result.get(property).isEmpty()) {
            return (Long) result.get(property).get(0);
        }
        return 0L;
    }

    /**
     * Get multiple Unsigned Integer values from WMI (as Longs)
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param properties
     *            A comma delimited list of properties whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @return A map, with each property as the key, containing Longs with the
     *         value of the requested properties. Each list's order corresponds
     *         to other lists.
     */
    public static Map<String, List<Long>> selectUint32sFrom(String namespace, String wmiClass, String properties,
            String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, properties,
                wmiClass, whereClause, UINT32_TYPE);
        HashMap<String, List<Long>> longMap = new HashMap<>();
        for (Entry<String, List<Object>> entry : result.entrySet()) {
            ArrayList<Long> longList = new ArrayList<>();
            for (Object obj : entry.getValue()) {
                longList.add((Long) obj);
            }
            longMap.put(entry.getKey(), longList);
        }
        return longMap;
    }

    /**
     * Get a single Float value from WMI
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param property
     *            The property whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @return A Float containing the value of the requested property
     */
    public static Float selectFloatFrom(String namespace, String wmiClass, String property, String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, property,
                wmiClass, whereClause, FLOAT_TYPE);
        if (result.containsKey(property) && !result.get(property).isEmpty()) {
            return (Float) result.get(property).get(0);
        }
        return 0f;
    }

    /**
     * Get multiple Float values from WMI
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param properties
     *            A comma delimited list of properties whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @return A map, with each property as the key, containing Floats with the
     *         value of the requested properties. Each list's order corresponds
     *         to other lists.
     */
    public static Map<String, List<Float>> selectFloatsFrom(String namespace, String wmiClass, String properties,
            String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, properties,
                wmiClass, whereClause, FLOAT_TYPE);
        HashMap<String, List<Float>> floatMap = new HashMap<>();
        for (Entry<String, List<Object>> entry : result.entrySet()) {
            ArrayList<Float> floatList = new ArrayList<>();
            for (Object obj : entry.getValue()) {
                floatList.add((Float) obj);
            }
            floatMap.put(entry.getKey(), floatList);
        }
        return floatMap;
    }

    /**
     * Get a single String value from WMI
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param property
     *            The property whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @return A string containing the value of the requested property
     */
    public static String selectStringFrom(String namespace, String wmiClass, String property, String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, property,
                wmiClass, whereClause, STRING_TYPE);
        if (result.containsKey(property) && !result.get(property).isEmpty()) {
            return (String) result.get(property).get(0);
        }
        return "";
    }

    /**
     * Get multiple String values from WMI
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param properties
     *            A comma delimited list of properties whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @return A map, with each property as the key, containing strings with the
     *         value of the requested properties. Each list's order corresponds
     *         to other lists.
     */
    public static Map<String, List<String>> selectStringsFrom(String namespace, String wmiClass, String properties,
            String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, properties,
                wmiClass, whereClause, STRING_TYPE);
        HashMap<String, List<String>> strMap = new HashMap<>();
        for (Entry<String, List<Object>> entry : result.entrySet()) {
            ArrayList<String> strList = new ArrayList<>();
            for (Object obj : entry.getValue()) {
                strList.add((String) obj);
            }
            strMap.put(entry.getKey(), strList);
        }
        return strMap;
    }

    /**
     * Get multiple individually typed values from WMI
     *
     * @param namespace
     *            The namespace or null to use the default
     * @param wmiClass
     *            The class to query
     * @param properties
     *            A comma delimited list of properties whose value to return
     * @param whereClause
     *            A WQL where clause matching properties and keywords
     * @param propertyTypes
     *            An array of types corresponding to the properties, or a single
     *            element array
     * @return A map, with each property as the key, containing Objects with the
     *         value of the requested properties. Each list's order corresponds
     *         to other lists. The type of the objects is identified by the
     *         propertyTypes array. If only one propertyType is given, all
     *         Objects will have that type. It is the responsibility of the
     *         caller to cast the returned objects.
     */
    public static Map<String, List<Object>> selectObjectsFrom(String namespace, String wmiClass, String properties,
            String whereClause, ValueType[] propertyTypes) {
        return queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, properties, wmiClass, whereClause,
                propertyTypes);
    }

    /**
     * Query WMI for values
     *
     * @param namespace
     *            The namespace to query
     * @param properties
     *            A single property or comma-delimited list of properties to
     *            enumerate
     * @param wmiClass
     *            The WMI class to query
     * @param propertyTypes
     *            An array corresponding to the properties, containing the type
     *            of data being queried, to control how VARIANT is parsed
     * @return A map, with the string value of each property as the key,
     *         containing a list of Objects which can be cast appropriately per
     *         valType. The order of objects in each list corresponds to the
     *         other lists.
     */
    private static Map<String, List<Object>> queryWMI(String namespace, String properties, String wmiClass,
            String whereClause, ValueType[] propertyTypes) {
        // Set up empty map
        Map<String, List<Object>> values = new HashMap<>();
        String[] props = properties.split(",");
        for (int i = 0; i < props.length; i++) {
            if ("__PATH".equals(props[i])) {
                // Methods will query __PATH
                values.put(propertyTypes[i].name(), new ArrayList<>());
            } else {
                // Properties are named
                values.put(props[i], new ArrayList<>());
            }
        }

        // Initialize COM
        if (!initCOM()) {
            unInitCOM();
            return values;
        }

        PointerByReference pSvc = new PointerByReference();
        if (!connectServer(namespace, pSvc)) {
            unInitCOM();
            return values;
        }
        WbemServices svc = new WbemServices(pSvc.getValue());

        PointerByReference pEnumerator = new PointerByReference();
        if (!selectProperties(svc, pEnumerator, properties, wmiClass, whereClause)) {
            svc.Release();
            unInitCOM();
            return values;
        }
        EnumWbemClassObject enumerator = new EnumWbemClassObject(pEnumerator.getValue());

        enumerateProperties(values, enumerator, props, propertyTypes, svc);

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
     * Steps 1 - 7 correspond to the above link.
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
            if (hres.intValue() == Ole32.RPC_E_CHANGED_MODE) {
                // Com already initialized, ignore error
                LOG.debug("COM already initialized.");
                securityInitialized = true;
                return true;
            }
            LOG.error(String.format("Failed to initialize COM library. Error code = 0x%08x", hres.intValue()));
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
        if (COMUtils.FAILED(hres) && hres.intValue() != Ole32.RPC_E_TOO_LATE) {
            LOG.error(String.format("Failed to initialize security. Error code = 0x%08x", hres.intValue()));
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
            if (!"root\\OpenHardwareMonitor".equals(namespace)) {
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
            LOG.error(String.format("Could not set proxy blanket. Error code = 0x%08x", hres.intValue()));
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
     * @param properties
     *            A comma separated list of properties to query
     * @param wmiClass
     *            The WMI class to query
     * @param whereClause
     *            A WHERE clause to narrow the query
     * @return True if successful. The enumerator will allow enumeration of
     *         results of the query
     */
    private static boolean selectProperties(WbemServices svc, PointerByReference pEnumerator, String properties,
            String wmiClass, String whereClause) {
        // Step 6: --------------------------------------------------
        // Use the IWbemServices pointer to make requests of WMI ----
        String query = String.format("SELECT %s FROM %s %s", properties, wmiClass,
                whereClause != null ? whereClause : "");
        LOG.debug("Query: {}", query);
        HRESULT hres = svc.ExecQuery(new BSTR("WQL"), new BSTR(query),
                new NativeLong(
                        EnumWbemClassObject.WBEM_FLAG_FORWARD_ONLY | EnumWbemClassObject.WBEM_FLAG_RETURN_IMMEDIATELY),
                null, pEnumerator);
        if (COMUtils.FAILED(hres)) {
            LOG.error(String.format("Query '%s' failed. Error code = 0x%08x", query, hres.intValue()));
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
     *            A map to hold the results of the query using the property as
     *            the key, and placing each enumerated result in a
     *            (common-index) list for each property
     * @param enumerator
     *            The enumerator with the results
     * @param properties
     *            Comma-delimited list of properties to retrieve
     * @param propertyTypes
     *            An array of property types matching the properties or a single
     *            property type which will be used for all properties
     * @param svc
     *            The WbemServices object
     */
    private static void enumerateProperties(Map<String, List<Object>> values, EnumWbemClassObject enumerator,
            String[] properties, ValueType[] propertyTypes, WbemServices svc) {
        if (propertyTypes.length > 1 && properties.length != propertyTypes.length) {
            throw new IllegalArgumentException("Property type array size must be 1 or equal to properties array size.");
        }
        // Step 7: -------------------------------------------------
        // Get the data from the query in step 6 -------------------
        PointerByReference pclsObj = new PointerByReference();
        LongByReference uReturn = new LongByReference(0L);
        int resultCount = 0;
        while (enumerator.getPointer() != Pointer.NULL) {
            HRESULT hres = enumerator.Next(new NativeLong(EnumWbemClassObject.WBEM_INFINITE), new NativeLong(1),
                    pclsObj, uReturn);
            // Requested 1; if 0 objects returned, we're done
            if (0L == uReturn.getValue() || COMUtils.FAILED(hres)) {
                // Enumerator will be released by calling method so no need to
                // release it here.
                LOG.debug(String.format("Returned %d results.", resultCount));
                return;
            }
            resultCount++;
            VARIANT.ByReference vtProp = new VARIANT.ByReference();

            // Get the value of the properties
            WbemClassObject clsObj = new WbemClassObject(pclsObj.getValue());
            for (int p = 0; p < properties.length; p++) {
                String property = properties[p];
                // hres =
                clsObj.Get(new BSTR(property), new NativeLong(0L), vtProp, null, null);

                ValueType propertyType = propertyTypes.length > 1 ? propertyTypes[p] : propertyTypes[0];
                switch (propertyType) {
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
                case PROCESS_GETOWNER:
                    // Win32_Process object GetOwner method
                    String owner = FormatUtil.join("\\",
                            execMethod(svc, vtProp.stringValue(), "GetOwner", "Domain", "User"));
                    values.get(propertyType.name()).add("\\".equals(owner) ? "N/A" : owner);
                    break;
                case PROCESS_GETOWNERSID:
                    // Win32_Process object GetOwnerSid method
                    String[] ownerSid = execMethod(svc, vtProp.stringValue(), "GetOwnerSid", "Sid");
                    values.get(propertyType.name()).add(ownerSid.length < 1 ? "" : ownerSid[0]);
                    break;
                default:
                    // Should never get here! If you get this exception you've
                    // added something to the enum without adding it here. Tsk.
                    throw new IllegalArgumentException("Unimplemented enum type: " + propertyType.toString());
                }
                OleAuto.INSTANCE.VariantClear(vtProp);
            }

            clsObj.Release();
        }
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

    /**
     * Convenience method for executing WMI methods without any input parameters
     *
     * @param svc
     *            The WbemServices object
     * @param clsObj
     *            The full path to the class object to execute (result of WMI
     *            "__PATH" query)
     * @param method
     *            The name of the method to execute
     * @param properties
     *            One or more properties returned as a result of the query
     * @return An array of the properties returned from the method
     */
    private static String[] execMethod(WbemServices svc, String clsObj, String method, String... properties) {
        List<String> result = new ArrayList<>();
        PointerByReference ppOutParams = new PointerByReference();
        HRESULT hres = svc.ExecMethod(new BSTR(clsObj), new BSTR(method), new NativeLong(0L), null, null, ppOutParams,
                null);
        if (COMUtils.FAILED(hres)) {
            return new String[0];
        }
        WbemClassObject obj = new WbemClassObject(ppOutParams.getValue());
        VARIANT.ByReference vtProp = new VARIANT.ByReference();
        for (String prop : properties) {
            hres = obj.Get(new BSTR(prop), new NativeLong(0L), vtProp, null, null);
            if (!COMUtils.FAILED(hres)) {
                result.add(vtProp.getValue() == null ? "" : vtProp.stringValue());
            }
        }
        obj.Release();
        return result.toArray(new String[result.size()]);
    }

}
