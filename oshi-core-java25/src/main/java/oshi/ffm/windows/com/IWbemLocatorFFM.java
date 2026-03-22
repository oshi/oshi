/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static oshi.ffm.windows.WindowsForeignFunctions.toWideString;

/**
 * FFM bindings for the IWbemLocator COM interface.
 * <p>
 * IWbemLocator is used to obtain the initial namespace pointer to IWbemServices
 * for WMI on a particular host computer.
 * </p>
 */
public final class IWbemLocatorFFM extends ComObjectFFM {

    private static final Logger LOG = LoggerFactory.getLogger(IWbemLocatorFFM.class);

    private IWbemLocatorFFM() {
    }

    /**
     * Creates an IWbemLocator instance.
     *
     * @param arena the arena for memory allocation
     * @return the IWbemLocator pointer, or empty if failed
     */
    public static Optional<MemorySegment> create(Arena arena) {
        MemorySegment clsid = GuidFFM.CLSID_WbemLocator(arena);
        MemorySegment iid = GuidFFM.IID_IWbemLocator(arena);
        MemorySegment pLocator = Ole32FFM.CoCreateInstance(arena, clsid, Ole32FFM.CLSCTX_INPROC_SERVER, iid);
        if (pLocator.equals(NULL)) {
            return Optional.empty();
        }
        return Optional.of(pLocator);
    }

    // IWbemLocator::ConnectServer function descriptor
    // HRESULT ConnectServer(
    //   BSTR strNetworkResource,  // namespace path
    //   BSTR strUser,             // user name (NULL for current)
    //   BSTR strPassword,         // password (NULL for current)
    //   BSTR strLocale,           // locale (NULL for current)
    //   long lSecurityFlags,      // security flags
    //   BSTR strAuthority,        // authority (NULL for NTLM)
    //   IWbemContext* pCtx,       // context (NULL)
    //   IWbemServices** ppNamespace // output
    // )
    private static final FunctionDescriptor CONNECT_SERVER_DESC = FunctionDescriptor.of(
            JAVA_INT,   // HRESULT return
            ADDRESS,    // this
            ADDRESS,    // strNetworkResource
            ADDRESS,    // strUser
            ADDRESS,    // strPassword
            ADDRESS,    // strLocale
            JAVA_INT,   // lSecurityFlags
            ADDRESS,    // strAuthority
            ADDRESS,    // pCtx
            ADDRESS     // ppNamespace
    );

    /**
     * Connects to the specified WMI namespace.
     *
     * @param pLocator  the IWbemLocator pointer
     * @param namespace the WMI namespace (e.g., "ROOT\\CIMV2")
     * @param arena     the arena for memory allocation
     * @return the IWbemServices pointer, or empty if failed
     */
    public static Optional<MemorySegment> connectServer(MemorySegment pLocator, String namespace, Arena arena) {
        if (pLocator == null || pLocator.equals(NULL)) {
            return Optional.empty();
        }
        MemorySegment bstrNamespace = null;
        try {
            MemorySegment vtable = getVtable(pLocator, arena);
            MemorySegment fnConnectServer = getVtableFunction(vtable, WbemcliFFM.IWBEMLOCATOR_CONNECTSERVER);
            MethodHandle mh = createDowncall(fnConnectServer, CONNECT_SERVER_DESC);

            // Allocate BSTR for namespace
            bstrNamespace = BStrFFM.fromString(arena, namespace);
            MemorySegment ppServices = arena.allocate(ADDRESS);

            int hr = (int) mh.invokeExact(
                    pLocator,
                    bstrNamespace,
                    NULL,           // user
                    NULL,           // password
                    NULL,           // locale
                    0,              // security flags
                    NULL,           // authority
                    NULL,           // context
                    ppServices
            );

            if (Ole32FFM.failed(hr)) {
                LOG.debug("IWbemLocator.ConnectServer failed with HRESULT: 0x{}", Integer.toHexString(hr));
                return Optional.empty();
            }

            return Optional.of(ppServices.get(ADDRESS, 0));
        } catch (Throwable t) {
            LOG.debug("IWbemLocatorFFM.connectServer failed", t);
            return Optional.empty();
        } finally {
            if (bstrNamespace != null) {
                BStrFFM.free(bstrNamespace);
            }
        }
    }
}
