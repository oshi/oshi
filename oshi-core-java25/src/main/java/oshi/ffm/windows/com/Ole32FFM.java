/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.WindowsForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.OptionalInt;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM bindings for OLE32.dll COM initialization and object creation functions.
 */
public final class Ole32FFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(Ole32FFM.class);

    private static final SymbolLookup OLE32 = lib("Ole32.dll");

    // COM initialization flags
    public static final int COINIT_APARTMENTTHREADED = 0x2;
    public static final int COINIT_MULTITHREADED = 0x0;
    public static final int COINIT_DISABLE_OLE1DDE = 0x4;
    public static final int COINIT_SPEED_OVER_MEMORY = 0x8;

    // CLSCTX flags
    public static final int CLSCTX_INPROC_SERVER = 0x1;
    public static final int CLSCTX_INPROC_HANDLER = 0x2;
    public static final int CLSCTX_LOCAL_SERVER = 0x4;
    public static final int CLSCTX_REMOTE_SERVER = 0x10;
    public static final int CLSCTX_SERVER = CLSCTX_INPROC_SERVER | CLSCTX_LOCAL_SERVER | CLSCTX_REMOTE_SERVER;

    // RPC authentication levels
    public static final int RPC_C_AUTHN_LEVEL_DEFAULT = 0;
    public static final int RPC_C_AUTHN_LEVEL_NONE = 1;
    public static final int RPC_C_AUTHN_LEVEL_CONNECT = 2;
    public static final int RPC_C_AUTHN_LEVEL_CALL = 3;
    public static final int RPC_C_AUTHN_LEVEL_PKT = 4;
    public static final int RPC_C_AUTHN_LEVEL_PKT_INTEGRITY = 5;
    public static final int RPC_C_AUTHN_LEVEL_PKT_PRIVACY = 6;

    // RPC impersonation levels
    public static final int RPC_C_IMP_LEVEL_DEFAULT = 0;
    public static final int RPC_C_IMP_LEVEL_ANONYMOUS = 1;
    public static final int RPC_C_IMP_LEVEL_IDENTIFY = 2;
    public static final int RPC_C_IMP_LEVEL_IMPERSONATE = 3;
    public static final int RPC_C_IMP_LEVEL_DELEGATE = 4;

    // EOLE authentication capabilities
    public static final int EOAC_NONE = 0x0;
    public static final int EOAC_DEFAULT = 0x800;

    // HRESULT values
    public static final int S_OK = 0;
    public static final int S_FALSE = 1;
    public static final int RPC_E_CHANGED_MODE = 0x80010106;
    public static final int RPC_E_TOO_LATE = 0x80010119;

    private Ole32FFM() {
    }

    // CoInitializeEx
    private static final MethodHandle CoInitializeEx = downcall(OLE32, "CoInitializeEx",
            JAVA_INT, ADDRESS, JAVA_INT);

    /**
     * Initializes the COM library for use by the calling thread.
     *
     * @param coInit the concurrency model and initialization options (COINIT_*)
     * @return HRESULT: S_OK if successful, S_FALSE if already initialized, or error code
     */
    public static OptionalInt CoInitializeEx(int coInit) {
        try {
            int hr = (int) CoInitializeEx.invokeExact(NULL, coInit);
            return OptionalInt.of(hr);
        } catch (Throwable t) {
            LOG.debug("Ole32FFM.CoInitializeEx failed", t);
            return OptionalInt.empty();
        }
    }

    // CoUninitialize
    private static final MethodHandle CoUninitialize = downcall(OLE32, "CoUninitialize", null);

    /**
     * Closes the COM library on the current thread.
     */
    public static void CoUninitialize() {
        try {
            CoUninitialize.invokeExact();
        } catch (Throwable t) {
            LOG.debug("Ole32FFM.CoUninitialize failed", t);
        }
    }

    // CoInitializeSecurity
    private static final MethodHandle CoInitializeSecurity = downcall(OLE32, "CoInitializeSecurity",
            JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS);

    /**
     * Registers security and sets the default security values for the process.
     *
     * @param authnLevel the default authentication level
     * @param impLevel   the default impersonation level
     * @param capabilities additional capabilities (EOAC_*)
     * @return HRESULT: S_OK if successful, RPC_E_TOO_LATE if already called, or error code
     */
    public static OptionalInt CoInitializeSecurity(int authnLevel, int impLevel, int capabilities) {
        try {
            int hr = (int) CoInitializeSecurity.invokeExact(
                    NULL,           // pSecDesc
                    -1,             // cAuthSvc
                    NULL,           // asAuthSvc
                    NULL,           // pReserved1
                    authnLevel,     // dwAuthnLevel
                    impLevel,       // dwImpLevel
                    NULL,           // pAuthList
                    capabilities,   // dwCapabilities
                    NULL            // pReserved3
            );
            return OptionalInt.of(hr);
        } catch (Throwable t) {
            LOG.debug("Ole32FFM.CoInitializeSecurity failed", t);
            return OptionalInt.empty();
        }
    }

    // CoCreateInstance
    private static final MethodHandle CoCreateInstance = downcall(OLE32, "CoCreateInstance",
            JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);

    /**
     * Creates a single uninitialized object of the class associated with a specified CLSID.
     *
     * @param arena   the arena for memory allocation
     * @param clsid   the CLSID of the object to create
     * @param clsctx  the context in which the code that manages the object will run (CLSCTX_*)
     * @param iid     the IID of the interface to obtain
     * @return the interface pointer, or NULL if failed
     */
    public static MemorySegment CoCreateInstance(Arena arena, MemorySegment clsid, int clsctx, MemorySegment iid) {
        try {
            MemorySegment ppv = arena.allocate(ADDRESS);
            int hr = (int) CoCreateInstance.invokeExact(clsid, NULL, clsctx, iid, ppv);
            if (hr != S_OK) {
                LOG.debug("CoCreateInstance failed with HRESULT: 0x{}", Integer.toHexString(hr));
                return NULL;
            }
            return ppv.get(ADDRESS, 0);
        } catch (Throwable t) {
            LOG.debug("Ole32FFM.CoCreateInstance failed", t);
            return NULL;
        }
    }

    // CoSetProxyBlanket
    private static final MethodHandle CoSetProxyBlanket = downcall(OLE32, "CoSetProxyBlanket",
            JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT);

    /**
     * Sets the authentication information for a proxy.
     *
     * @param pProxy     the proxy to set authentication on
     * @param authnSvc   the authentication service (use -1 for default)
     * @param authzSvc   the authorization service (use -1 for default)
     * @param authnLevel the authentication level
     * @param impLevel   the impersonation level
     * @param capabilities additional capabilities
     * @return HRESULT
     */
    public static OptionalInt CoSetProxyBlanket(MemorySegment pProxy, int authnSvc, int authzSvc,
            int authnLevel, int impLevel, int capabilities) {
        try {
            int hr = (int) CoSetProxyBlanket.invokeExact(
                    pProxy,
                    authnSvc,       // dwAuthnSvc
                    authzSvc,       // dwAuthzSvc
                    NULL,           // pServerPrincName
                    authnLevel,     // dwAuthnLevel
                    impLevel,       // dwImpLevel
                    NULL,           // pAuthInfo
                    capabilities    // dwCapabilities
            );
            return OptionalInt.of(hr);
        } catch (Throwable t) {
            LOG.debug("Ole32FFM.CoSetProxyBlanket failed", t);
            return OptionalInt.empty();
        }
    }

    /**
     * Checks if an HRESULT indicates success (non-negative value).
     *
     * @param hr the HRESULT value
     * @return true if successful
     */
    public static boolean succeeded(int hr) {
        return hr >= 0;
    }

    /**
     * Checks if an HRESULT indicates failure (negative value).
     *
     * @param hr the HRESULT value
     * @return true if failed
     */
    public static boolean failed(int hr) {
        return hr < 0;
    }
}
