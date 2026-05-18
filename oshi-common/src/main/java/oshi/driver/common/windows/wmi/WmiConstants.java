/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

/**
 * WMI type constants used for interpreting {@link WmiResult} values.
 */
public final class WmiConstants {

    private WmiConstants() {
    }

    // CIM types (from Wbemcli.h)
    /** CIM type for signed 32-bit integer. */
    public static final int CIM_SINT32 = 3;
    /** CIM type for 32-bit real. */
    public static final int CIM_REAL32 = 4;
    /** CIM type for string. */
    public static final int CIM_STRING = 8;
    /** CIM type for unsigned 16-bit integer. */
    public static final int CIM_UINT16 = 18;
    /** CIM type for unsigned 32-bit integer. */
    public static final int CIM_UINT32 = 19;
    /** CIM type for unsigned 64-bit integer. */
    public static final int CIM_UINT64 = 21;
    /** CIM type for datetime. */
    public static final int CIM_DATETIME = 101;
    /** CIM type for reference. */
    public static final int CIM_REFERENCE = 102;

    // Variant types (from OAIdl.h)
    /** Variant type for 32-bit integer. */
    public static final int VT_I4 = 3;
    /** Variant type for 32-bit real. */
    public static final int VT_R4 = 4;
    /** Variant type for BSTR (string). */
    public static final int VT_BSTR = 8;
}
