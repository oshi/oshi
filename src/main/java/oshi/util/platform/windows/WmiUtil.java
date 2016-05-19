/**
 * Oshi (https://github.com/dblock/oshi)
 *
 * Copyright (c) 2010 - 2016 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.util.platform.windows;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import oshi.util.ParseUtil;

/**
 * Provides access to WMI queries
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(WmiUtil.class);

    public static final String DEFAULT_NAMESPACE = "ROOT\\CIMV2";

    private static boolean securityInitialized = false;

    /**
     * Enum for WMI queries for proper parsing from the returned VARIANT
     */
    public enum ValueType {
        STRING, LONG, FLOAT, DATETIME
    }

    /**
     * Get a single Long value from WMI
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
    public static Long selectLongFrom(String namespace, String wmiClass, String property, String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, property,
                wmiClass, whereClause, ValueType.LONG);
        if (result.containsKey(property) && !result.get(property).isEmpty()) {
            return (Long) result.get(property).get(0);
        }
        return 0L;
    }

    /**
     * Get multiple Long values from WMI
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
    public static Map<String, List<Long>> selectLongsFrom(String namespace, String wmiClass, String properties,
            String whereClause) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, properties,
                wmiClass, whereClause, ValueType.LONG);
        HashMap<String, List<Long>> longMap = new HashMap<>();
        for (String key : result.keySet()) {
            ArrayList<Long> longList = new ArrayList<>();
            for (Object obj : result.get(key)) {
                longList.add((Long) obj);
            }
            longMap.put(key, longList);
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
                wmiClass, whereClause, ValueType.FLOAT);
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
                wmiClass, whereClause, ValueType.FLOAT);
        HashMap<String, List<Float>> floatMap = new HashMap<>();
        for (String key : result.keySet()) {
            ArrayList<Float> floatList = new ArrayList<>();
            for (Object obj : result.get(key)) {
                floatList.add((Float) obj);
            }
            floatMap.put(key, floatList);
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
                wmiClass, whereClause, ValueType.STRING);
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
                wmiClass, whereClause, ValueType.STRING);
        HashMap<String, List<String>> strMap = new HashMap<>();
        for (String key : result.keySet()) {
            ArrayList<String> strList = new ArrayList<>();
            for (Object obj : result.get(key)) {
                strList.add((String) obj);
            }
            strMap.put(key, strList);
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
     *            An array of types corresponding to the properties
     * @return A map, with each property as the key, containing Objects with the
     *         value of the requested properties. Each list's order corresponds
     *         to other lists. The type of the objects is identified by the
     *         propertyTypes array. It is the responsibility of the caller to
     *         cast the returned objects.
     */
    public static Map<String, List<Object>> selectObjectsFrom(String namespace, String wmiClass, String properties,
            String whereClause, ValueType[] propertyTypes) {
        Map<String, List<Object>> result = queryWMI(namespace == null ? DEFAULT_NAMESPACE : namespace, properties,
                wmiClass, whereClause, propertyTypes);
        return result;
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
     * @param valType
     *            The type of data being queried, to control how VARIANT is
     *            parsed
     * @return A map, with the string value of each property as the key,
     *         containing a list of Objects which can be cast appropriately per
     *         valType. The order of objects in each list corresponds to the
     *         other lists.
     */
    private static Map<String, List<Object>> queryWMI(String namespace, String properties, String wmiClass,
            String whereClause, ValueType valType) {
        // Set up empty map
        String[] props = properties.split(",");
        ValueType[] propertyTypes = new ValueType[props.length];
        for (int i = 0; i < props.length; i++) {
            propertyTypes[i] = valType;
        }
        return queryWMI(namespace, properties, wmiClass, whereClause, propertyTypes);
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
            values.put(props[i], new ArrayList<Object>());
        }

        // Initialize COM
        if (!initCOM()) {
            Ole32.INSTANCE.CoUninitialize();
            return values;
        }

        PointerByReference pSvc = new PointerByReference();
        if (!connectServer(namespace, pSvc)) {
            Ole32.INSTANCE.CoUninitialize();
            return values;
        }
        WbemServices svc = new WbemServices(pSvc.getValue());

        PointerByReference pEnumerator = new PointerByReference();
        if (!selectProperties(svc, pEnumerator, properties, wmiClass, whereClause)) {
            svc.Release();
            Ole32.INSTANCE.CoUninitialize();
            return values;
        }
        EnumWbemClassObject enumerator = new EnumWbemClassObject(pEnumerator.getValue());

        enumerateProperties(values, enumerator, props, propertyTypes);

        // Cleanup
        enumerator.Release();
        svc.Release();
        Ole32.INSTANCE.CoUninitialize();
        return values;
    }

    /*
     * Getting WMI Data from Local Computer
     * 
     * Ported from:
     * https://msdn.microsoft.com/en-us/library/aa390423(v=VS.85).aspx
     */

    private static boolean initCOM() {
        // Step 1: --------------------------------------------------
        // Initialize COM. ------------------------------------------
        HRESULT hres = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
        if (COMUtils.FAILED(hres)) {
            LOG.error(String.format("Failed to initialize COM library. Error code = 0x%08x", hres.intValue()));
            return false;
        }
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
            LOG.error(String.format("Could not connect to namespace %s. Error code = 0x%08x", namespace,
                    hres.intValue()));
            loc.Release();
            Ole32.INSTANCE.CoUninitialize();
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
            Ole32.INSTANCE.CoUninitialize();
            return false;
        }
        return true;
    }

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
            Ole32.INSTANCE.CoUninitialize();
            return false;
        }
        return true;
    }

    private static void enumerateProperties(Map<String, List<Object>> values, EnumWbemClassObject enumerator,
            String[] properties, ValueType[] propertyTypes) {
        if (properties.length != propertyTypes.length) {
            throw new IllegalArgumentException("Property type array size must equal properties array size.");
        }
        // Step 7: -------------------------------------------------
        // Get the data from the query in step 6 -------------------
        PointerByReference pclsObj = new PointerByReference();
        LongByReference uReturn = new LongByReference(0L);
        while (enumerator.getPointer() != Pointer.NULL) {
            HRESULT hres = enumerator.Next(new NativeLong(EnumWbemClassObject.WBEM_INFINITE), new NativeLong(1),
                    pclsObj, uReturn);
            // Requested 1; if 0 objects returned, we're done
            if (0L == uReturn.getValue() || COMUtils.FAILED(hres)) {
                enumerator.Release();
                return;
            }
            VARIANT.ByReference vtProp = new VARIANT.ByReference();

            // Get the value of the properties
            WbemClassObject clsObj = new WbemClassObject(pclsObj.getValue());
            for (int p = 0; p < properties.length; p++) {
                String property = properties[p];
                hres = clsObj.Get(new BSTR(property), new NativeLong(0L), vtProp, null, null);

                switch (propertyTypes[p]) {
                case STRING:
                    values.get(property).add(vtProp.getValue() == null ? "unknown" : vtProp.stringValue());
                    break;
                case LONG: // WinDef.LONG
                    values.get(property)
                            .add(vtProp.getValue() == null ? 0L : vtProp._variant.__variant.lVal.longValue());
                    break;
                case FLOAT:
                    values.get(property).add(vtProp.getValue() == null ? 0f : vtProp.floatValue());
                    break;
                case DATETIME:
                    // Read a string in format 20160513072950.782000-420 and
                    // parse to a long representing Date.getTime()
                    if (vtProp.getValue() != null) {
                        // Parse the date including milliseconds
                        Date date = ParseUtil.cimDateTimeToDate(vtProp.stringValue());
                        if (date != null) {
                            values.get(property).add(date);
                            break;
                        }
                    }
                    values.get(property).add(new Date(0));
                    break;
                default:
                    // Should never get here!
                    LOG.error("Unimplemented enum type.");
                }
                OleAuto.INSTANCE.VariantClear(vtProp.getPointer());
            }
            clsObj.Release();
        }
    }
}