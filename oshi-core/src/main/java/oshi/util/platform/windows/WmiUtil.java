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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.COM.COMUtils;

import oshi.jna.platform.windows.Ole32;
import oshi.jna.platform.windows.Wbemcli;
import oshi.jna.platform.windows.Wbemcli.IEnumWbemClassObject;
import oshi.jna.platform.windows.Wbemcli.IWbemServices;
import oshi.jna.platform.windows.Wbemcli.WbemcliException;
import oshi.jna.platform.windows.WbemcliUtil;
import oshi.jna.platform.windows.WbemcliUtil.WmiQuery;
import oshi.jna.platform.windows.WbemcliUtil.WmiResult;

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

    // Cache open WMI namespace connections
    private static Map<String, WmiConnection> connectionCache = new HashMap<>();
    private static long nextCacheClear = System.currentTimeMillis() + connectionCacheTimeout;

    // Cache failed wmi classes
    private static Set<String> failedWmiClassNames = new HashSet<>();
    // Not a built in manespace, failed connections are normal and don't need
    // error logging
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

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
        return WbemcliUtil.hasNamespace(namespace);
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
     *            an enum
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

            // Connect to the server
            WmiConnection conn = connectToNamespace(query.getNameSpace());

            // Send query
            IEnumWbemClassObject enumerator = WbemcliUtil.selectProperties(conn.getService(), query);

            result = WbemcliUtil.enumerateProperties(enumerator, query.getPropertyEnum(), wmiTimeout);
            enumerator.Release();
        } catch (WbemcliException e) {
            // Ignore any exceptions with OpenHardwareMonitor
            if (!OHM_NAMESPACE.equals(query.getNameSpace())) {
                switch (e.getErrorCode()) {
                case Wbemcli.WBEM_E_INVALID_NAMESPACE:
                    LOG.error("WMI Failed connecting to namespace {}", query.getNameSpace());
                    break;
                case Wbemcli.WBEM_E_INVALID_CLASS:
                    LOG.warn("WMI class {} not found, ignoring further requests.", query.getWmiClassName());
                    failedWmiClassNames.add(query.getWmiClassName());
                    break;
                case Wbemcli.WBEM_E_INVALID_QUERY:
                    LOG.error("Invalid Query: SELECT {} FROM {}.", query.getPropertyEnum().getEnumConstants(),
                            query.getWmiClassName());
                    break;
                default:
                    // Any other errors
                    LOG.error(e.getMessage());
                }
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
            nextCacheClear = System.currentTimeMillis() + connectionCacheTimeout;
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
        IWbemServices svc = WbemcliUtil.connectServer(namespace);
        WmiConnection conn = INSTANCE.new WmiConnection(svc);
        // Add to cache
        connectionCache.put(namespace, conn);
        return conn;
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
                throw new Wbemcli.WbemcliException("Failed to initialize COM library.", hres.intValue());
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
                throw new Wbemcli.WbemcliException("Failed to initialize security.", hres.intValue());
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

    /**
     * Gets the connection cache timeout. WMI connections will be released if
     * older than this number of milliseconds.
     * 
     * @return Returns the connectionCacheTimeout.
     */
    public static int getConnectionCacheTimeout() {
        return connectionCacheTimeout;
    }

    /**
     * Sets the connection cache timeout. WMI connections will be released if
     * older than this number of milliseconds.
     * 
     * @param connectionCacheTimeout
     *            The connectionCacheTimeout to set.
     */
    public static void setConnectionCacheTimeout(int connectionCacheTimeout) {
        WmiUtil.connectionCacheTimeout = connectionCacheTimeout;
    }
}