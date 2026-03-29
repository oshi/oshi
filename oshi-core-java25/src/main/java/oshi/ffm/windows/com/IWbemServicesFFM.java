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

/**
 * FFM bindings for the IWbemServices COM interface.
 * <p>
 * IWbemServices is used to execute WMI queries and access WMI objects.
 * </p>
 */
public final class IWbemServicesFFM extends ComObjectFFM {

    private static final Logger LOG = LoggerFactory.getLogger(IWbemServicesFFM.class);

    /**
     * WQL query language identifier.
     */
    public static final String WQL = "WQL";

    private IWbemServicesFFM() {
    }

    // IWbemServices::ExecQuery function descriptor
    // HRESULT ExecQuery(
    // BSTR strQueryLanguage, // "WQL"
    // BSTR strQuery, // query string
    // long lFlags, // flags
    // IWbemContext* pCtx, // context (NULL)
    // IEnumWbemClassObject** ppEnum // output enumerator
    // )
    private static final FunctionDescriptor EXEC_QUERY_DESC = FunctionDescriptor.of(JAVA_INT, // HRESULT return
            ADDRESS, // this
            ADDRESS, // strQueryLanguage
            ADDRESS, // strQuery
            JAVA_INT, // lFlags
            ADDRESS, // pCtx
            ADDRESS // ppEnum
    );

    /**
     * Executes a WQL query.
     *
     * @param pServices the IWbemServices pointer
     * @param query     the WQL query string (e.g., "SELECT * FROM Win32_LogicalDisk")
     * @param flags     query flags (e.g., WBEM_FLAG_FORWARD_ONLY | WBEM_FLAG_RETURN_IMMEDIATELY)
     * @param arena     the arena for memory allocation
     * @return the IEnumWbemClassObject enumerator, or empty if failed
     */
    public static Optional<MemorySegment> execQuery(MemorySegment pServices, String query, int flags, Arena arena) {
        if (pServices == null || pServices.equals(NULL)) {
            return Optional.empty();
        }
        MemorySegment bstrQueryLang = NULL;
        MemorySegment bstrQuery = NULL;
        try {
            MemorySegment vtable = getVtable(pServices, arena);
            MemorySegment fnExecQuery = getVtableFunction(vtable, WbemcliFFM.IWBEMSERVICES_EXECQUERY);
            MethodHandle mh = createDowncall(fnExecQuery, EXEC_QUERY_DESC);

            // Allocate BSTRs
            bstrQueryLang = BStrFFM.fromString(arena, WQL);
            bstrQuery = BStrFFM.fromString(arena, query);
            MemorySegment ppEnum = arena.allocate(ADDRESS);

            int hr = (int) mh.invokeExact(pServices, bstrQueryLang, bstrQuery, flags, NULL, // context
                    ppEnum);

            if (Ole32FFM.failed(hr)) {
                LOG.debug("IWbemServices.ExecQuery failed with HRESULT: 0x{}", Integer.toHexString(hr));
                return Optional.empty();
            }

            return Optional.of(ppEnum.get(ADDRESS, 0));
        } catch (Throwable t) {
            LOG.debug("IWbemServicesFFM.execQuery failed", t);
            return Optional.empty();
        } finally {
            if (!bstrQueryLang.equals(NULL)) {
                BStrFFM.free(bstrQueryLang);
            }
            if (!bstrQuery.equals(NULL)) {
                BStrFFM.free(bstrQuery);
            }
        }
    }

    /**
     * Executes a WQL query with default flags (FORWARD_ONLY | RETURN_IMMEDIATELY).
     *
     * @param pServices the IWbemServices pointer
     * @param query     the WQL query string
     * @param arena     the arena for memory allocation
     * @return the IEnumWbemClassObject enumerator, or empty if failed
     */
    public static Optional<MemorySegment> execQuery(MemorySegment pServices, String query, Arena arena) {
        return execQuery(pServices, query, WbemcliFFM.WBEM_FLAG_FORWARD_ONLY | WbemcliFFM.WBEM_FLAG_RETURN_IMMEDIATELY,
                arena);
    }
}
