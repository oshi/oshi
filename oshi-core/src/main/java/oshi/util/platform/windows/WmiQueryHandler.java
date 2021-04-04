/*
 * MIT License
 *
 * Copyright (c) 2010 - 2021 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util.platform.windows;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.platform.win32.Ole32; // NOSONAR
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.COM.COMException;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.GlobalConfig;

/**
 * Utility to handle WMI Queries. Designed to be extended with user-customized
 * behavior.
 */
@ThreadSafe
public class WmiQueryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(WmiQueryHandler.class);

    private static int globalTimeout = GlobalConfig.get("oshi.util.wmi.timeout", -1);

    static {
        if (globalTimeout == 0 || globalTimeout < -1) {
            throw new GlobalConfig.PropertyException("oshi.util.wmi.timeout");
        }
    }

    // Timeout for WMI queries
    protected int wmiTimeout = globalTimeout;

    // Cache failed wmi classes
    protected final Set<String> failedWmiClassNames = new HashSet<>();

    // Preferred threading model
    private int comThreading = Ole32.COINIT_MULTITHREADED;

    // Track initialization of Security
    private boolean securityInitialized = false;

    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    // Factory to create this or a subclass
    private static Class<? extends WmiQueryHandler> customClass = null;

    /**
     * Factory method to create an instance of this class. To override this class,
     * use {@link #setInstanceClass(Class)} to define a subclass which extends
     * {@link oshi.util.platform.windows.WmiQueryHandler}.
     *
     * @return An instance of this class or a class defined by
     *         {@link #setInstanceClass(Class)}
     */
    public static synchronized WmiQueryHandler createInstance() {
        if (customClass == null) {
            return new WmiQueryHandler();
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
     * Define a subclass to be instantiated by {@link #createInstance()}. The class
     * must extend {@link oshi.util.platform.windows.WmiQueryHandler}.
     *
     * @param instanceClass
     *            The class to instantiate with {@link #createInstance()}.
     */
    public static synchronized void setInstanceClass(Class<? extends WmiQueryHandler> instanceClass) {
        customClass = instanceClass;
    }

    /**
     * Query WMI for values. Makes no assumptions on whether the user has previously
     * initialized COM.
     *
     * @param <T>
     *            WMI queries use an Enum to identify the fields to query, and use
     *            the enum values as keys to retrieve the results.
     * @param query
     *            A WmiQuery object encapsulating the namespace, class, and
     *            properties
     * @return a WmiResult object containing the query results, wrapping an EnumMap
     */
    public <T extends Enum<T>> WbemcliUtil.WmiResult<T> queryWMI(WbemcliUtil.WmiQuery<T> query) {
        return queryWMI(query, true);
    }

    /**
     * Query WMI for values.
     *
     * @param <T>
     *            WMI queries use an Enum to identify the fields to query, and use
     *            the enum values as keys to retrieve the results.
     * @param query
     *            A WmiQuery object encapsulating the namespace, class, and
     *            properties
     * @param initCom
     *            Whether to initialize COM. If {@code true}, initializes COM before
     *            the query and uninitializes after. If {@code false}, assumes the
     *            user has initialized COM separately. This can improve WMI query
     *            performance.
     * @return a WmiResult object containing the query results, wrapping an EnumMap
     */
    public <T extends Enum<T>> WbemcliUtil.WmiResult<T> queryWMI(WbemcliUtil.WmiQuery<T> query, boolean initCom) {
        WbemcliUtil.WmiResult<T> result = WbemcliUtil.INSTANCE.new WmiResult<>(query.getPropertyEnum());
        if (failedWmiClassNames.contains(query.getWmiClassName())) {
            return result;
        }
        boolean comInit = false;
        try {
            if (initCom) {
                comInit = initCOM();
            }
            result = query.execute(wmiTimeout);
        } catch (COMException e) {
            // Ignore any exceptions with OpenHardwareMonitor
            if (!WmiUtil.OHM_NAMESPACE.equals(query.getNameSpace())) {
                final int hresult = e.getHresult() == null ? -1 : e.getHresult().intValue();
                switch (hresult) {
                case Wbemcli.WBEM_E_INVALID_NAMESPACE:
                    LOG.warn("COM exception: Invalid Namespace {}", query.getNameSpace());
                    break;
                case Wbemcli.WBEM_E_INVALID_CLASS:
                    LOG.warn("COM exception: Invalid Class {}", query.getWmiClassName());
                    break;
                case Wbemcli.WBEM_E_INVALID_QUERY:
                    LOG.warn("COM exception: Invalid Query: {}", WmiUtil.queryToString(query));
                    break;
                default:
                    handleComException(query, e);
                    break;
                }
                failedWmiClassNames.add(query.getWmiClassName());
            }
        } catch (TimeoutException e) {
            LOG.warn("WMI query timed out after {} ms: {}", wmiTimeout, WmiUtil.queryToString(query));
        }
        if (comInit) {
            unInitCOM();
        }
        return result;
    }

    /**
     * COM Exception handler. Logs a warning message.
     *
     * @param query
     *            a {@link com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery}
     *            object.
     * @param ex
     *            a {@link com.sun.jna.platform.win32.COM.COMException} object.
     */
    protected void handleComException(WbemcliUtil.WmiQuery<?> query, COMException ex) {
        LOG.warn(
                "COM exception querying {}, which might not be on your system. Will not attempt to query it again. Error was {}: {}",
                query.getWmiClassName(), ex.getHresult().intValue(), ex.getMessage());
    }

    /**
     * Initializes COM library and sets security to impersonate the local user
     *
     * @return True if COM was initialized and needs to be uninitialized, false
     *         otherwise
     */
    public boolean initCOM() {
        boolean comInit = false;
        // Step 1: --------------------------------------------------
        // Initialize COM. ------------------------------------------
        comInit = initCOM(getComThreading());
        if (!comInit) {
            comInit = initCOM(switchComThreading());
        }
        // Step 2: --------------------------------------------------
        // Set general COM security levels --------------------------
        if (comInit && !isSecurityInitialized()) {
            WinNT.HRESULT hres = Ole32.INSTANCE.CoInitializeSecurity(null, -1, null, null,
                    Ole32.RPC_C_AUTHN_LEVEL_DEFAULT, Ole32.RPC_C_IMP_LEVEL_IMPERSONATE, null, Ole32.EOAC_NONE, null);
            // If security already initialized we get RPC_E_TOO_LATE
            // This can be safely ignored
            if (COMUtils.FAILED(hres) && hres.intValue() != WinError.RPC_E_TOO_LATE) {
                Ole32.INSTANCE.CoUninitialize();
                throw new COMException("Failed to initialize security.", hres);
            }
            securityInitialized = true;
        }
        return comInit;
    }

    /**
     * Initializes COM with a specific threading model
     *
     * @param coInitThreading
     *            The threading model
     * @return True if COM was initialized and needs to be uninitialized, false
     *         otherwise
     */
    protected boolean initCOM(int coInitThreading) {
        WinNT.HRESULT hres = Ole32.INSTANCE.CoInitializeEx(null, coInitThreading);
        switch (hres.intValue()) {
        // Successful local initialization (S_OK) or was already initialized
        // (S_FALSE) but still needs uninit
        case COMUtils.S_OK:
        case COMUtils.S_FALSE:
            return true;
        // COM was already initialized with a different threading model
        case WinError.RPC_E_CHANGED_MODE:
            return false;
        // Any other results is impossible
        default:
            throw new COMException("Failed to initialize COM library.", hres);
        }
    }

    /**
     * UnInitializes COM library. This should be called once for every successful
     * call to initCOM.
     */
    public void unInitCOM() {
        Ole32.INSTANCE.CoUninitialize();
    }

    /**
     * Returns the current threading model for COM initialization, as OSHI is
     * required to match if an external program has COM initialized already.
     *
     * @return The current threading model
     */
    public int getComThreading() {
        return comThreading;
    }

    /**
     * Switches the current threading model for COM initialization, as OSHI is
     * required to match if an external program has COM initialized already.
     *
     * @return The new threading model after switching
     */
    public int switchComThreading() {
        if (comThreading == Ole32.COINIT_APARTMENTTHREADED) {
            comThreading = Ole32.COINIT_MULTITHREADED;
        } else {
            comThreading = Ole32.COINIT_APARTMENTTHREADED;
        }
        return comThreading;
    }

    /**
     * Security only needs to be initialized once. This boolean identifies whether
     * that has happened.
     *
     * @return Returns the securityInitialized.
     */
    public boolean isSecurityInitialized() {
        return securityInitialized;
    }

    /**
     * Gets the current WMI timeout. WMI queries will fail if they take longer than
     * this number of milliseconds. A value of -1 is infinite (no timeout).
     *
     * @return Returns the current value of wmiTimeout.
     */
    public int getWmiTimeout() {
        return wmiTimeout;
    }

    /**
     * Sets the WMI timeout. WMI queries will fail if they take longer than this
     * number of milliseconds.
     *
     * @param wmiTimeout
     *            The wmiTimeout to set, in milliseconds. To disable timeouts, set
     *            timeout as -1 (infinite).
     */
    public void setWmiTimeout(int wmiTimeout) {
        this.wmiTimeout = wmiTimeout;
    }
}
