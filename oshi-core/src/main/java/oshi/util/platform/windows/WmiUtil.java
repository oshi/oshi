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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;

import oshi.jna.platform.windows.Ole32;
import oshi.jna.platform.windows.Wbemcli;
import oshi.jna.platform.windows.Wbemcli.IWbemServices;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;
import oshi.util.ParseUtil;

/**
 * Helper class for WMI
 * 
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {
    /**
     * Instance to generate the WmiConnection class.
     */
    public static final WmiUtil INSTANCE = new WmiUtil();

    private static final Logger LOG = LoggerFactory.getLogger(WmiUtil.class);

    // Global timeout for WMI queries
    private static int wmiTimeout = Wbemcli.WBEM_INFINITE;
    private static int connectionCacheTimeout = 300_000;

    // Cache namespaces
    private static Set<String> hasNamespaceCache = new HashSet<>();
    private static Set<String> hasNotNamespaceCache = new HashSet<>();

    // Cache failed wmi classes
    private static Set<String> failedWmiClassNames = new HashSet<>();
    // Not a built in manespace, failed connections are normal and don't need
    // error logging
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

    private static final String CLASS_CAST_MSG = "%s is not a %s type. CIM Type is %d and VT type is %d";

    // Track initialization of COM and Security
    private static boolean comInitialized = false;
    private static boolean securityInitialized = false;

    /**
     * Private constructor so this class can't be instantiated from the outside.
     * Also initializes COM and sets up hooks to uninit if necessary.
     */
    private WmiUtil() {
        // Initialize COM
        initCOM();

        // Set up hook to uninit on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                unInitCOM();
            }
        });
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
        if (WbemcliUtil.hasNamespace(namespace)) {
            hasNamespaceCache.add(namespace);
            return true;
        }
        hasNotNamespaceCache.add(namespace);
        return false;
    }

    /**
     * Helper class for WMI connection caching
     */
    public class WmiConnection {
        private long staleAfter;
        private IWbemServices svc;

        WmiConnection(IWbemServices svc) {
            this.svc = svc;
            refresh();
        }

        public IWbemServices getService() {
            return this.svc;
        }

        public boolean isStale() {
            return System.currentTimeMillis() > staleAfter;
        }

        public void refresh() {
            this.staleAfter = System.currentTimeMillis() + connectionCacheTimeout;
        }

        public void close() {
            this.svc.Release();
        }
    }

    /**
     * Query WMI for values, with no timeout.
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param query
     *            A WmiQuery object encapsulating the namespace, class, and
     *            properties
     * @return a WmiResult object containing the query results, wrapping an
     *         EnumMap
     */
    public static <T extends Enum<T>> WmiResult<T> queryWMI(WmiQuery<T> query) {

        WmiResult<T> result = WbemcliUtil.INSTANCE.new WmiResult<>(query.getPropertyEnum());
        if (failedWmiClassNames.contains(query.getWmiClassName())) {
            return result;
        }
        try {
            // Initialize COM if not already done. Needed if COM was previously
            // initialized externally but is no longer initialized.
            if (!isComInitialized()) {
                initCOM();
            }

            result = query.execute(wmiTimeout);
        } catch (COMException e) {
            // Ignore any exceptions with OpenHardwareMonitor
            if (!OHM_NAMESPACE.equals(query.getNameSpace())) {
                // TODO: JNA 5 version of COMException will include the HResult
                // and allow finer grained error messages based on
                // Wbemcli.WBEM_E_INVALID_NAMESPACE,
                // Wbemcli.WBEM_E_INVALID_CLASS, or
                // Wbemcli.WBEM_E_INVALID_QUERY.
                failedWmiClassNames.add(query.getWmiClassName());
                LOG.warn(
                        "COM exception querying {}, which might not be on your system. Will not attempt to query it again. Error was: {}:",
                        query.getWmiClassName(), e.getMessage());
            }
        } catch (TimeoutException e) {
            T[] props = query.getPropertyEnum().getEnumConstants();
            StringBuilder sb = new StringBuilder("SELECT ");
            sb.append(props[0].name());
            for (int i = 1; i < props.length; i++) {
                sb.append(',').append(props[i].name());
            }
            sb.append(" FROM ").append(query.getWmiClassName());
            LOG.error("WMI query timed out after {} ms: {}", wmiTimeout, sb);
        }
        return result;
    }

    /**
     * Gets a String value from a WmiResult
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_STRING) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "String",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Date value from a WmiResult as a String
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getDateString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_DATETIME) {
            String date = getStr(result, property, index);
            return date.substring(0, 4) + '-' + date.substring(4, 6) + '-' + date.substring(6, 8);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "DateTime",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Reference value from a WmiResult as a String
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getRefString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_REFERENCE) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "Reference",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> String getStr(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return "";
        } else if (result.getVtType(property) == Variant.VT_BSTR) {
            return (String) o;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "String-mapped",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Uint64 value from a WmiResult (parsing the String). Note that
     * while the CIM type is unsigned, the return type is signed and the parsing
     * will exclude any return values above Long.MAX_VALUE.
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null and parseable as a long, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint64(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0L;
        } else if (result.getCIMType(property) == Wbemcli.CIM_UINT64 && result.getVtType(property) == Variant.VT_BSTR) {
            return ParseUtil.parseLongOrDefault((String) o, 0L);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT64",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets an UINT32 value from a WmiResult. Note that while a UINT32 CIM type
     * is unsigned, the return type is signed and requires further processing by
     * the user if unsigned values are desired.
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint32(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_UINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets an UINT32 value from a WmiResult as a long, preserving the
     * unsignedness.
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint32asLong(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_UINT32) {
            return getInt(result, property, index) & 0xFFFFFFFFL;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Sint32 value from a WmiResult. Note that while the CIM type is
     * unsigned, the return type is signed and requires further processing by
     * the user if unsigned values are desired.
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getSint32(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_SINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "SINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Uint16 value from a WmiResult. Note that while the CIM type is
     * unsigned, the return type is signed and requires further processing by
     * the user if unsigned values are desired.
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint16(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_UINT16) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT16",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> int getInt(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0;
        } else if (result.getVtType(property) == Variant.VT_I4) {
            return (int) o;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "32-bit integer",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Float value from a WmiResult
     * 
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> float getFloat(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0f;
        } else if (result.getCIMType(property) == Wbemcli.CIM_REAL32 && result.getVtType(property) == Variant.VT_R4) {
            return (float) o;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "Float",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Initializes COM library and sets security to impersonate the local user
     */
    public static void initCOM() {
        HRESULT hres = null;
        // Step 1: --------------------------------------------------
        // Initialize COM. ------------------------------------------
        if (!isComInitialized()) {
            hres = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
            switch (hres.intValue()) {
            // Successful local initialization
            case COMUtils.S_OK:
                comInitialized = true;
                break;
            // COM was already initialized
            case COMUtils.S_FALSE:
            case WinError.RPC_E_CHANGED_MODE:
                break;
            // Any other results is an error
            default:
                throw new COMException("Failed to initialize COM library.");
            }
        }
        // Step 2: --------------------------------------------------
        // Set general COM security levels --------------------------
        if (!isSecurityInitialized()) {
            hres = Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null, Ole32.RPC_C_AUTHN_LEVEL_DEFAULT,
                    Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
            // If security already initialized we get RPC_E_TOO_LATE
            // This can be safely ignored
            if (COMUtils.FAILED(hres) && hres.intValue() != WinError.RPC_E_TOO_LATE) {
                Ole32.INSTANCE.CoUninitialize();
                throw new COMException("Failed to initialize security.");
            }
            securityInitialized = true;
        }
    }

    /**
     * UnInitializes COM library if it was initialized by the {@link #initCOM()}
     * method. Otherwise, does nothing.
     */
    public static void unInitCOM() {
        if (isComInitialized()) {
            Ole32.INSTANCE.CoUninitialize();
            comInitialized = false;
        }
    }

    /**
     * COM may already have been initialized outside this class. This boolean is
     * a flag whether this class initialized it, to avoid uninitializing later
     * and killing the external initialization
     * 
     * @return Returns whether this class initialized COM
     */
    public static boolean isComInitialized() {
        return comInitialized;
    }

    /**
     * Security only needs to be initialized once. This boolean identifies
     * whether that has happened.
     * 
     * @return Returns the securityInitialized.
     */
    public static boolean isSecurityInitialized() {
        return securityInitialized;
    }

    /**
     * Gets the current WMI timeout. WMI queries will fail if they take longer
     * than this number of milliseconds. A value of -1 is infinite (no timeout).
     * 
     * @return Returns the current value of wmiTimeout.
     */
    public static int getWmiTimeout() {
        return wmiTimeout;
    }

    /**
     * Sets the WMI timeout. WMI queries will fail if they take longer than this
     * number of milliseconds.
     * 
     * @param wmiTimeout
     *            The wmiTimeout to set, in milliseconds. To disable timeouts,
     *            set timeout as -1 (infinite).
     */
    public static void setWmiTimeout(int wmiTimeout) {
        WmiUtil.wmiTimeout = wmiTimeout;
    }
}