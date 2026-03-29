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
import java.util.OptionalInt;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM bindings for the IEnumWbemClassObject COM interface.
 * <p>
 * IEnumWbemClassObject is used to enumerate WMI objects returned by a query.
 * </p>
 */
public final class IEnumWbemClassObjectFFM extends ComObjectFFM {

    private static final Logger LOG = LoggerFactory.getLogger(IEnumWbemClassObjectFFM.class);

    private IEnumWbemClassObjectFFM() {
    }

    // IEnumWbemClassObject::Next function descriptor
    // HRESULT Next(
    // long lTimeout, // timeout in milliseconds
    // ULONG uCount, // number of objects to retrieve
    // IWbemClassObject** apObjects, // array of object pointers
    // ULONG* puReturned // number of objects returned
    // )
    private static final FunctionDescriptor NEXT_DESC = FunctionDescriptor.of(JAVA_INT, // HRESULT return
            ADDRESS, // this
            JAVA_INT, // lTimeout
            JAVA_INT, // uCount
            ADDRESS, // apObjects
            ADDRESS // puReturned
    );

    /**
     * Result of a Next() call.
     *
     * @param hresult the HRESULT from the Next() call
     * @param pObject the retrieved object pointer, or NULL if none
     */
    public record NextResult(int hresult, MemorySegment pObject) {
        public boolean hasObject() {
            return Ole32FFM.succeeded(hresult) && pObject != null && !pObject.equals(NULL);
        }

        public boolean isComplete() {
            return hresult == WbemcliFFM.WBEM_S_FALSE;
        }
    }

    /**
     * Retrieves the next object in the enumeration.
     *
     * @param pEnum   the IEnumWbemClassObject pointer
     * @param timeout timeout in milliseconds (use WBEM_INFINITE for no timeout)
     * @param arena   the arena for memory allocation
     * @return the next result containing HRESULT and object pointer
     */
    public static NextResult next(MemorySegment pEnum, int timeout, Arena arena) {
        if (pEnum == null || pEnum.equals(NULL)) {
            return new NextResult(WbemcliFFM.WBEM_E_FAILED, NULL);
        }
        try {
            MemorySegment vtable = getVtable(pEnum, arena);
            MemorySegment fnNext = getVtableFunction(vtable, WbemcliFFM.IENUMWBEMCLASSOBJECT_NEXT);
            MethodHandle mh = createDowncall(fnNext, NEXT_DESC);

            MemorySegment apObjects = arena.allocate(ADDRESS);
            MemorySegment puReturned = arena.allocate(JAVA_INT);

            int hr = (int) mh.invokeExact(pEnum, timeout, 1, // retrieve one object at a time
                    apObjects, puReturned);

            int returned = puReturned.get(JAVA_INT, 0);
            MemorySegment pObject = returned > 0 ? apObjects.get(ADDRESS, 0) : NULL;

            return new NextResult(hr, pObject);
        } catch (Throwable t) {
            LOG.debug("IEnumWbemClassObjectFFM.next failed", t);
            return new NextResult(WbemcliFFM.WBEM_E_FAILED, NULL);
        }
    }

    /**
     * Retrieves the next object with default timeout (10 seconds).
     *
     * @param pEnum the IEnumWbemClassObject pointer
     * @param arena the arena for memory allocation
     * @return the next result
     */
    public static NextResult next(MemorySegment pEnum, Arena arena) {
        return next(pEnum, 10000, arena);
    }

    // IEnumWbemClassObject::Reset function descriptor
    private static final FunctionDescriptor RESET_DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    /**
     * Resets the enumeration to the beginning.
     *
     * @param pEnum the IEnumWbemClassObject pointer
     * @param arena the arena for memory allocation
     * @return HRESULT
     */
    public static OptionalInt reset(MemorySegment pEnum, Arena arena) {
        if (pEnum == null || pEnum.equals(NULL)) {
            return OptionalInt.empty();
        }
        try {
            MemorySegment vtable = getVtable(pEnum, arena);
            MemorySegment fnReset = getVtableFunction(vtable, WbemcliFFM.IENUMWBEMCLASSOBJECT_RESET);
            MethodHandle mh = createDowncall(fnReset, RESET_DESC);
            int hr = (int) mh.invokeExact(pEnum);
            return OptionalInt.of(hr);
        } catch (Throwable t) {
            LOG.debug("IEnumWbemClassObjectFFM.reset failed", t);
            return OptionalInt.empty();
        }
    }
}
