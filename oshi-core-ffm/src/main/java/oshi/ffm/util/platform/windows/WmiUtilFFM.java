/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.windows;

import java.time.OffsetDateTime;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.ffm.windows.com.VariantFFM;
import oshi.ffm.windows.com.WbemcliFFM;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Helper class for WMI using FFM-based WmiResult.
 */
@ThreadSafe
public final class WmiUtilFFM {

    private static final String CLASS_CAST_MSG = "%s is not a %s type. CIM Type is %d and VT type is %d";

    private WmiUtilFFM() {
    }

    /**
     * Gets a String value from a WmiResult.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getString(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_STRING) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "String",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Date value from a WmiResult as a String in ISO 8601 format.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getDateString(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        OffsetDateTime dateTime = getDateTime(result, property, index);
        if (dateTime.equals(Constants.UNIX_EPOCH)) {
            return "";
        }
        return dateTime.toLocalDate().toString();
    }

    /**
     * Gets a DateTime value from a WmiResult as an OffsetDateTime.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, otherwise {@link Constants#UNIX_EPOCH}
     */
    public static <T extends Enum<T>> OffsetDateTime getDateTime(WbemcliUtilFFM.WmiResult<T> result, T property,
            int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_DATETIME) {
            return ParseUtil.parseCimDateTimeToOffset(getStr(result, property, index));
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "DateTime",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Reference value from a WmiResult as a String.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getRefString(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_REFERENCE) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "Reference",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> String getStr(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return "";
        } else if (result.getVtType(property) == VariantFFM.VT_BSTR) {
            return (String) o;
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "String-mapped",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Uint64 value from a WmiResult (parsing the String).
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null and parseable as a long, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint64(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0L;
        } else if (o instanceof Number) {
            return ((Number) o).longValue();
        } else if (result.getCIMType(property) == WbemcliFFM.CIM_UINT64
                && result.getVtType(property) == VariantFFM.VT_BSTR) {
            return ParseUtil.parseLongOrDefault((String) o, 0L);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT64",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a UINT32 value from a WmiResult.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint32(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_UINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a UINT32 value from a WmiResult as a long, preserving the unsignedness.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint32asLong(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_UINT32) {
            return getInt(result, property, index) & 0xFFFFFFFFL;
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Sint32 value from a WmiResult.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getSint32(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_SINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "SINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Uint16 value from a WmiResult.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint16(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == WbemcliFFM.CIM_UINT16) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT16",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> int getInt(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0;
        }
        int vt = result.getVtType(property);
        if (vt == VariantFFM.VT_I4 || vt == VariantFFM.VT_INT || vt == VariantFFM.VT_UI4 || vt == VariantFFM.VT_UINT
                || vt == VariantFFM.VT_I2 || vt == VariantFFM.VT_UI2 || vt == VariantFFM.VT_I1
                || vt == VariantFFM.VT_UI1) {
            return ((Number) o).intValue();
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "32-bit integer",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Float value from a WmiResult.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> float getFloat(WbemcliUtilFFM.WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0f;
        } else if (result.getCIMType(property) == WbemcliFFM.CIM_REAL32
                && result.getVtType(property) == VariantFFM.VT_R4) {
            return (float) o;
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "Float",
                result.getCIMType(property), result.getVtType(property)));
    }
}
