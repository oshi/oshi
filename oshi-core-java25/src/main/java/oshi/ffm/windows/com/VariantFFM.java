/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.windows.com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.ffm.windows.WindowsForeignFunctions;

import java.lang.foreign.Arena;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

/**
 * FFM representation of the Windows VARIANT structure used in COM/OLE automation.
 * <p>
 * VARIANT is a tagged union that can hold different types of data.
 * </p>
 */
public final class VariantFFM extends WindowsForeignFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(VariantFFM.class);

    private static final SymbolLookup OLEAUT32 = lib("OleAut32.dll");

    // VARENUM - Variant type constants
    public static final int VT_EMPTY = 0;
    public static final int VT_NULL = 1;
    public static final int VT_I2 = 2;
    public static final int VT_I4 = 3;
    public static final int VT_R4 = 4;
    public static final int VT_R8 = 5;
    public static final int VT_CY = 6;
    public static final int VT_DATE = 7;
    public static final int VT_BSTR = 8;
    public static final int VT_DISPATCH = 9;
    public static final int VT_ERROR = 10;
    public static final int VT_BOOL = 11;
    public static final int VT_VARIANT = 12;
    public static final int VT_UNKNOWN = 13;
    public static final int VT_DECIMAL = 14;
    public static final int VT_I1 = 16;
    public static final int VT_UI1 = 17;
    public static final int VT_UI2 = 18;
    public static final int VT_UI4 = 19;
    public static final int VT_I8 = 20;
    public static final int VT_UI8 = 21;
    public static final int VT_INT = 22;
    public static final int VT_UINT = 23;
    public static final int VT_VOID = 24;
    public static final int VT_HRESULT = 25;
    public static final int VT_PTR = 26;
    public static final int VT_SAFEARRAY = 27;
    public static final int VT_CARRAY = 28;
    public static final int VT_USERDEFINED = 29;
    public static final int VT_LPSTR = 30;
    public static final int VT_LPWSTR = 31;
    public static final int VT_ARRAY = 0x2000;
    public static final int VT_BYREF = 0x4000;

    /**
     * The memory layout of a VARIANT structure (24 bytes on 64-bit).
     * Layout: vt (2) + wReserved1 (2) + wReserved2 (2) + wReserved3 (2) + data (16)
     */
    public static final StructLayout LAYOUT = MemoryLayout.structLayout(
            JAVA_SHORT.withName("vt"),
            JAVA_SHORT.withName("wReserved1"),
            JAVA_SHORT.withName("wReserved2"),
            JAVA_SHORT.withName("wReserved3"),
            MemoryLayout.unionLayout(
                    JAVA_LONG.withName("llVal"),
                    JAVA_INT.withName("lVal"),
                    JAVA_SHORT.withName("iVal"),
                    JAVA_DOUBLE.withName("dblVal"),
                    ADDRESS.withName("bstrVal"),
                    ADDRESS.withName("punkVal")
            ).withName("data"),
            MemoryLayout.paddingLayout(8) // Padding to align to 24 bytes
    );

    /**
     * Size of a VARIANT in bytes.
     */
    public static final long SIZE = 24; // Fixed size for 64-bit

    /**
     * Offset of the vt field.
     */
    public static final long VT_OFFSET = 0;

    /**
     * Offset of the data union.
     */
    public static final long DATA_OFFSET = 8;

    private VariantFFM() {
    }

    /**
     * Allocates and initializes an empty VARIANT.
     *
     * @param arena the arena for memory allocation
     * @return a memory segment containing the initialized VARIANT
     */
    public static MemorySegment allocate(Arena arena) {
        MemorySegment variant = arena.allocate(SIZE);
        variant.fill((byte) 0);
        return variant;
    }

    /**
     * Gets the variant type (vt field).
     *
     * @param variant the VARIANT memory segment
     * @return the variant type
     */
    public static int getVt(MemorySegment variant) {
        return variant.get(JAVA_SHORT, VT_OFFSET) & 0xFFFF;
    }

    /**
     * Gets the BSTR value from a VARIANT.
     *
     * @param variant the VARIANT memory segment
     * @param arena   the arena for string conversion
     * @return the string value, or empty string if null
     */
    public static String getBstrVal(MemorySegment variant, Arena arena) {
        MemorySegment bstr = variant.get(ADDRESS, DATA_OFFSET);
        if (bstr.equals(MemorySegment.NULL)) {
            return "";
        }
        return BStrFFM.toString(bstr, arena);
    }

    /**
     * Gets the integer value from a VARIANT (VT_I4, VT_INT, VT_UI4, VT_UINT).
     *
     * @param variant the VARIANT memory segment
     * @return the integer value
     */
    public static int getIntVal(MemorySegment variant) {
        return variant.get(JAVA_INT, DATA_OFFSET);
    }

    /**
     * Gets the long value from a VARIANT (VT_I8, VT_UI8).
     *
     * @param variant the VARIANT memory segment
     * @return the long value
     */
    public static long getLongVal(MemorySegment variant) {
        return variant.get(JAVA_LONG, DATA_OFFSET);
    }

    /**
     * Gets the short value from a VARIANT (VT_I2, VT_UI2, VT_BOOL).
     *
     * @param variant the VARIANT memory segment
     * @return the short value
     */
    public static short getShortVal(MemorySegment variant) {
        return variant.get(JAVA_SHORT, DATA_OFFSET);
    }

    /**
     * Gets the double value from a VARIANT (VT_R8).
     *
     * @param variant the VARIANT memory segment
     * @return the double value
     */
    public static double getDoubleVal(MemorySegment variant) {
        return variant.get(JAVA_DOUBLE, DATA_OFFSET);
    }

    /**
     * Gets the float value from a VARIANT (VT_R4).
     *
     * @param variant the VARIANT memory segment
     * @return the float value
     */
    public static float getFloatVal(MemorySegment variant) {
        return variant.get(JAVA_FLOAT, DATA_OFFSET);
    }

    /**
     * Gets the boolean value from a VARIANT (VT_BOOL).
     * In COM, VARIANT_TRUE is -1 and VARIANT_FALSE is 0.
     *
     * @param variant the VARIANT memory segment
     * @return the boolean value
     */
    public static boolean getBoolVal(MemorySegment variant) {
        return getShortVal(variant) != 0;
    }

    // VariantClear
    private static final MethodHandle VariantClear = downcall(OLEAUT32, "VariantClear", JAVA_INT, ADDRESS);

    /**
     * Clears a VARIANT, releasing any resources.
     *
     * @param variant the VARIANT to clear
     * @return HRESULT
     */
    public static int clear(MemorySegment variant) {
        try {
            return (int) VariantClear.invokeExact(variant);
        } catch (Throwable t) {
            LOG.debug("VariantFFM.clear failed", t);
            return -1;
        }
    }
}
