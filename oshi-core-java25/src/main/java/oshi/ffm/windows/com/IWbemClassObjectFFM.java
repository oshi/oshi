/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.ParseUtil;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * FFM bindings for the IWbemClassObject COM interface.
 * <p>
 * IWbemClassObject represents a single WMI object (row) in a query result.
 * </p>
 */
public final class IWbemClassObjectFFM extends ComObjectFFM {

    private static final Logger LOG = LoggerFactory.getLogger(IWbemClassObjectFFM.class);

    private IWbemClassObjectFFM() {
    }

    // IWbemClassObject::Get function descriptor
    // HRESULT Get(
    //   LPCWSTR wszName,          // property name
    //   long lFlags,              // flags (0)
    //   VARIANT* pVal,            // output value
    //   CIMTYPE* pType,           // output CIM type (optional)
    //   long* plFlavor            // output flavor (optional)
    // )
    private static final FunctionDescriptor GET_DESC = FunctionDescriptor.of(
            JAVA_INT,   // HRESULT return
            ADDRESS,    // this
            ADDRESS,    // wszName
            JAVA_INT,   // lFlags
            ADDRESS,    // pVal
            ADDRESS,    // pType
            ADDRESS     // plFlavor
    );

    /**
     * Result of a Get() call containing the VARIANT and CIM type.
     */
    public record GetResult(int hresult, MemorySegment variant, int cimType) {
        public boolean succeeded() {
            return Ole32FFM.succeeded(hresult);
        }
    }

    /**
     * Gets a property value from a WMI object.
     *
     * @param pObject      the IWbemClassObject pointer
     * @param propertyName the property name
     * @param arena        the arena for memory allocation
     * @return the get result containing HRESULT, VARIANT, and CIM type
     */
    public static GetResult get(MemorySegment pObject, String propertyName, Arena arena) {
        if (pObject == null || pObject.equals(NULL)) {
            return new GetResult(WbemcliFFM.WBEM_E_FAILED, NULL, WbemcliFFM.CIM_ILLEGAL);
        }
        try {
            MemorySegment vtable = getVtable(pObject, arena);
            MemorySegment fnGet = getVtableFunction(vtable, WbemcliFFM.IWBEMCLASSOBJECT_GET);
            MethodHandle mh = createDowncall(fnGet, GET_DESC);

            MemorySegment wszName = oshi.ffm.windows.WindowsForeignFunctions.toWideString(arena, propertyName);
            MemorySegment pVal = VariantFFM.allocate(arena);
            MemorySegment pType = arena.allocate(JAVA_INT);

            int hr = (int) mh.invokeExact(
                    pObject,
                    wszName,
                    0,              // flags
                    pVal,
                    pType,
                    NULL            // flavor (not needed)
            );

            // Only read pType on success; use CIM_ILLEGAL on failure
            int cimType = Ole32FFM.succeeded(hr) ? pType.get(JAVA_INT, 0) : WbemcliFFM.CIM_ILLEGAL;
            return new GetResult(hr, pVal, cimType);
        } catch (Throwable t) {
            LOG.debug("IWbemClassObjectFFM.get failed", t);
            return new GetResult(WbemcliFFM.WBEM_E_FAILED, NULL, WbemcliFFM.CIM_ILLEGAL);
        }
    }

    /**
     * Gets a string property value from a WMI object.
     *
     * @param pObject      the IWbemClassObject pointer
     * @param propertyName the property name
     * @param arena        the arena for memory allocation
     * @return the string value, or empty string if failed or null
     */
    public static String getString(MemorySegment pObject, String propertyName, Arena arena) {
        GetResult result = get(pObject, propertyName, arena);
        // Always clear variant in finally, even on failed HRESULT
        try {
            if (!result.succeeded()) {
                return "";
            }
            int vt = VariantFFM.getVt(result.variant());
            if (vt == VariantFFM.VT_NULL || vt == VariantFFM.VT_EMPTY) {
                return "";
            }
            if (vt == VariantFFM.VT_BSTR) {
                return VariantFFM.getBstrVal(result.variant(), arena);
            }
            LOG.debug("Expected VT_BSTR but got VT type: {}", vt);
            return "";
        } finally {
            VariantFFM.clear(result.variant());
        }
    }

    /**
     * Gets an integer property value from a WMI object (VT_I4, VT_I2, VT_UI4, VT_UI2).
     *
     * @param pObject      the IWbemClassObject pointer
     * @param propertyName the property name
     * @param arena        the arena for memory allocation
     * @return the integer value, or 0 if failed or null
     */
    public static int getInt(MemorySegment pObject, String propertyName, Arena arena) {
        GetResult result = get(pObject, propertyName, arena);
        // Always clear variant in finally, even on failed HRESULT
        try {
            if (!result.succeeded()) {
                return 0;
            }
            int vt = VariantFFM.getVt(result.variant());
            if (vt == VariantFFM.VT_NULL || vt == VariantFFM.VT_EMPTY) {
                return 0;
            }
            return switch (vt) {
                // Signed types: sign-extend to int
                case VariantFFM.VT_I4, VariantFFM.VT_INT -> VariantFFM.getIntVal(result.variant());
                case VariantFFM.VT_I2 -> VariantFFM.getShortVal(result.variant()); // sign-extends automatically
                case VariantFFM.VT_I1 -> VariantFFM.getByteVal(result.variant()); // sign-extends automatically
                // Unsigned types: zero-extend to int
                case VariantFFM.VT_UI4, VariantFFM.VT_UINT -> VariantFFM.getIntVal(result.variant());
                case VariantFFM.VT_UI2 -> VariantFFM.getShortVal(result.variant()) & 0xFFFF;
                case VariantFFM.VT_UI1 -> VariantFFM.getByteVal(result.variant()) & 0xFF;
                default -> {
                    LOG.debug("Expected integer VT type but got: {}", vt);
                    yield 0;
                }
            };
        } finally {
            VariantFFM.clear(result.variant());
        }
    }

    /**
     * Gets a long property value from a WMI object (VT_I8, VT_UI8, or BSTR for UINT64).
     *
     * @param pObject      the IWbemClassObject pointer
     * @param propertyName the property name
     * @param arena        the arena for memory allocation
     * @return the long value, or 0 if failed or null
     */
    public static long getLong(MemorySegment pObject, String propertyName, Arena arena) {
        GetResult result = get(pObject, propertyName, arena);
        // Always clear variant in finally, even on failed HRESULT
        try {
            if (!result.succeeded()) {
                return 0L;
            }
            int vt = VariantFFM.getVt(result.variant());
            if (vt == VariantFFM.VT_NULL || vt == VariantFFM.VT_EMPTY) {
                return 0L;
            }
            // CIM_SINT64/CIM_UINT64 are returned as VT_BSTR
            if (vt == VariantFFM.VT_BSTR) {
                String strVal = VariantFFM.getBstrVal(result.variant(), arena);
                // Use signed or unsigned parsing based on CIM type
                if (result.cimType() == WbemcliFFM.CIM_SINT64) {
                    return ParseUtil.parseLongOrDefault(strVal, 0L);
                }
                return ParseUtil.parseUnsignedLongOrDefault(strVal, 0L);
            }
            // 64-bit types
            if (vt == VariantFFM.VT_I8 || vt == VariantFFM.VT_UI8) {
                return VariantFFM.getLongVal(result.variant());
            }
            // Signed 32-bit: sign-extend to long
            if (vt == VariantFFM.VT_I4 || vt == VariantFFM.VT_INT) {
                return VariantFFM.getIntVal(result.variant()); // sign-extends automatically
            }
            // Unsigned 32-bit: zero-extend to long
            if (vt == VariantFFM.VT_UI4 || vt == VariantFFM.VT_UINT) {
                return Integer.toUnsignedLong(VariantFFM.getIntVal(result.variant()));
            }
            LOG.debug("Expected long VT type but got: {}", vt);
            return 0L;
        } finally {
            VariantFFM.clear(result.variant());
        }
    }

    /**
     * Gets a boolean property value from a WMI object (VT_BOOL).
     *
     * @param pObject      the IWbemClassObject pointer
     * @param propertyName the property name
     * @param arena        the arena for memory allocation
     * @return the boolean value, or false if failed or null
     */
    public static boolean getBoolean(MemorySegment pObject, String propertyName, Arena arena) {
        GetResult result = get(pObject, propertyName, arena);
        // Always clear variant in finally, even on failed HRESULT
        try {
            if (!result.succeeded()) {
                return false;
            }
            int vt = VariantFFM.getVt(result.variant());
            if (vt == VariantFFM.VT_NULL || vt == VariantFFM.VT_EMPTY) {
                return false;
            }
            if (vt == VariantFFM.VT_BOOL) {
                return VariantFFM.getBoolVal(result.variant());
            }
            LOG.debug("Expected VT_BOOL but got VT type: {}", vt);
            return false;
        } finally {
            VariantFFM.clear(result.variant());
        }
    }
}
