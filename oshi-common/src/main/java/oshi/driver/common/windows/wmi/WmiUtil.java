/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import static oshi.driver.common.windows.wmi.WmiConstants.CIM_DATETIME;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_REAL32;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_REFERENCE;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_SINT32;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_STRING;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT16;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT32;
import static oshi.driver.common.windows.wmi.WmiConstants.CIM_UINT64;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_BSTR;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_I4;
import static oshi.driver.common.windows.wmi.WmiConstants.VT_R4;

import java.time.OffsetDateTime;
import java.util.Locale;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.Constants;
import oshi.util.ParseUtil;

/**
 * Helper class for extracting typed values from {@link WmiResult} objects.
 */
@ThreadSafe
public final class WmiUtil {

    /** The namespace where Open Hardware Monitor publishes to WMI. */
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

    /** The namespace where LibreHardwareMonitor publishes to WMI. */
    public static final String LHM_NAMESPACE = "ROOT\\LibreHardwareMonitor";

    private static final String CLASS_CAST_MSG = "%s is not a %s type. CIM Type is %d and VT type is %d";

    private WmiUtil() {
    }

    /**
     * Translate a WmiQuery to the actual query string.
     *
     * @param <T>   the property enum type
     * @param query The WmiQuery object
     * @return The string that is queried in WMI
     */
    public static <T extends Enum<T>> String queryToString(WmiQuery<T> query) {
        T[] props = query.getPropertyEnum().getEnumConstants();
        if (props.length == 0) {
            throw new IllegalArgumentException("Property enum must have at least one constant");
        }
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(props[0].name());
        for (int i = 1; i < props.length; i++) {
            sb.append(',').append(props[i].name());
        }
        sb.append(" FROM ").append(query.getWmiClassName());
        return sb.toString();
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
    public static <T extends Enum<T>> String getString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_STRING) {
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
    public static <T extends Enum<T>> String getDateString(WmiResult<T> result, T property, int index) {
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
    public static <T extends Enum<T>> OffsetDateTime getDateTime(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_DATETIME) {
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
    public static <T extends Enum<T>> String getRefString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_REFERENCE) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "Reference",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> String getStr(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return "";
        } else if (result.getVtType(property) == VT_BSTR) {
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
    public static <T extends Enum<T>> long getUint64(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0L;
        } else if (result.getCIMType(property) == CIM_UINT64 && result.getVtType(property) == VT_BSTR) {
            return ParseUtil.parseLongOrDefault((String) o, 0L);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT64",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets an UINT32 value from a WmiResult.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint32(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_UINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets an UINT32 value from a WmiResult as a long, preserving the unsignedness.
     *
     * @param <T>      the property enum type
     * @param result   The WmiResult from which to fetch the value
     * @param property The property (column) to fetch
     * @param index    The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint32asLong(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_UINT32) {
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
    public static <T extends Enum<T>> int getSint32(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_SINT32) {
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
    public static <T extends Enum<T>> int getUint16(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == CIM_UINT16) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "UINT16",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> int getInt(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0;
        } else if (result.getVtType(property) == VT_I4) {
            return (int) o;
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
    public static <T extends Enum<T>> float getFloat(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0f;
        } else if (result.getCIMType(property) == CIM_REAL32 && result.getVtType(property) == VT_R4) {
            return (float) o;
        }
        throw new ClassCastException(String.format(Locale.ROOT, CLASS_CAST_MSG, property.name(), "Float",
                result.getCIMType(property), result.getVtType(property)));
    }
}
