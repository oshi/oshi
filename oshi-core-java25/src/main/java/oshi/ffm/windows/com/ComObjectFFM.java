/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.ForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Base class for COM object operations via FFM.
 * <p>
 * Provides utilities for vtable access and IUnknown interface methods.
 * </p>
 */
public class ComObjectFFM extends ForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(ComObjectFFM.class);

    /**
     * Size of a pointer in bytes (8 on 64-bit systems).
     */
    protected static final long PTR_SIZE = ADDRESS.byteSize();

    protected ComObjectFFM() {
    }

    /**
     * Gets the vtable pointer from a COM object.
     *
     * @param pObject the COM object pointer
     * @param arena   the arena for memory reinterpretation
     * @return the vtable memory segment
     */
    protected static MemorySegment getVtable(MemorySegment pObject, Arena arena) {
        if (pObject == null || pObject.equals(NULL)) {
            return NULL;
        }
        // COM object layout: first pointer is the vtable pointer
        MemorySegment vtablePtr = pObject.reinterpret(PTR_SIZE, arena, null).get(ADDRESS, 0);
        // Reinterpret with enough slots for IWbemLocator (7 methods) and IWbemServices (30 methods);
        // 32 slots covers all COM interfaces used by this implementation
        return vtablePtr.reinterpret(PTR_SIZE * 32, arena, null);
    }

    /**
     * Gets a function pointer from a vtable at the specified index.
     *
     * @param vtable the vtable memory segment
     * @param index  the function index (0-based)
     * @return the function pointer
     */
    protected static MemorySegment getVtableFunction(MemorySegment vtable, int index) {
        return vtable.get(ADDRESS, (long) index * PTR_SIZE);
    }

    /**
     * Creates a downcall handle for a vtable function.
     *
     * @param fnPtr      the function pointer
     * @param descriptor the function descriptor
     * @return the method handle
     */
    protected static MethodHandle createDowncall(MemorySegment fnPtr, FunctionDescriptor descriptor) {
        return Linker.nativeLinker().downcallHandle(fnPtr, descriptor);
    }

    // IUnknown::AddRef
    private static final FunctionDescriptor ADDREF_DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS);

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
            MemorySegment vtable = getVtable(pObject, arena);
            MemorySegment fnAddRef = getVtableFunction(vtable, WbemcliFFM.IUNKNOWN_ADDREF);
            MethodHandle mh = createDowncall(fnAddRef, ADDREF_DESC);
            return (int) mh.invokeExact(pObject);
        } catch (Throwable t) {
            LOG.debug("ComObjectFFM.addRef failed", t);
            return 0;
        }
    }

    // IUnknown::Release
    private static final FunctionDescriptor RELEASE_DESC = FunctionDescriptor.of(JAVA_INT, ADDRESS);

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
            MemorySegment vtable = getVtable(pObject, arena);
            MemorySegment fnRelease = getVtableFunction(vtable, WbemcliFFM.IUNKNOWN_RELEASE);
            MethodHandle mh = createDowncall(fnRelease, RELEASE_DESC);
            return (int) mh.invokeExact(pObject);
        } catch (Throwable t) {
            LOG.debug("ComObjectFFM.release failed", t);
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
