/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.ForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM bindings for the COM IUnknown interface (QueryInterface, AddRef, Release).
 * <p>
 * All COM interfaces inherit from IUnknown, so these vtable slots (0, 1, 2) are present on every COM object.
 * </p>
 */
public class IUnknownFFM extends ForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(IUnknownFFM.class);

    protected IUnknownFFM() {
    }

    // IUnknown::AddRef — vtable slot 1
    private static final FunctionDescriptor ADDREF_DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    // IUnknown::Release — vtable slot 2
    private static final FunctionDescriptor RELEASE_DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS);

    /**
     * Increments the reference count of a COM object.
     *
     * @param pObject the COM object
     * @param arena   the arena for vtable access
     * @return the new reference count
     */
    public static int addRef(MemorySegment pObject, Arena arena) {
        if (pObject == null || pObject.equals(NULL)) {
            return 0;
        }
        try {
            MemorySegment vtable = ComObjectFFM.getVtable(pObject, arena);
            MemorySegment fnAddRef = ComObjectFFM.getVtableFunction(vtable, WbemcliFFM.IUNKNOWN_ADDREF);
            MethodHandle mh = ComObjectFFM.createDowncall(fnAddRef, ADDREF_DESC);
            return (int) mh.invokeExact(pObject);
        } catch (Throwable t) {
            LOG.debug("IUnknownFFM.addRef failed", t);
            return 0;
        }
    }

    /**
     * Decrements the reference count of a COM object.
     *
     * @param pObject the COM object
     * @param arena   the arena for vtable access
     * @return the new reference count
     */
    public static int release(MemorySegment pObject, Arena arena) {
        if (pObject == null || pObject.equals(NULL)) {
            return 0;
        }
        try {
            MemorySegment vtable = ComObjectFFM.getVtable(pObject, arena);
            MemorySegment fnRelease = ComObjectFFM.getVtableFunction(vtable, WbemcliFFM.IUNKNOWN_RELEASE);
            MethodHandle mh = ComObjectFFM.createDowncall(fnRelease, RELEASE_DESC);
            return (int) mh.invokeExact(pObject);
        } catch (Throwable t) {
            LOG.debug("IUnknownFFM.release failed", t);
            return 0;
        }
    }

    /**
     * Safely releases a COM object, ignoring null pointers.
     *
     * @param pObject the COM object to release
     * @param arena   the arena for vtable access
     */
    public static void safeRelease(MemorySegment pObject, Arena arena) {
        if (pObject != null && !pObject.equals(NULL)) {
            release(pObject, arena);
        }
    }
}
