/**
 * Oshi (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2018 The Oshi Project Team
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Maintainers:
 * dblock[at]dblock[dot]org
 * widdis[at]gmail[dot]com
 * enrico.bianchi[at]gmail[dot]com
 *
 * Contributors:
 * https://github.com/oshi/oshi/graphs/contributors
 */
package oshi.util.platform.windows;

import com.sun.jna.platform.win32.Variant;
import com.sun.jna.platform.win32.COM.Wbemcli;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.ParseUtil;

/**
 * Helper class for WMI
 *
 * @author widdis[at]gmail[dot]com
 */
public class WmiUtil {

    private static WmiQueryHandler SHARED;

    // Not a built in manespace, failed connections are normal and don't need
    // error logging
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

    private static final String CLASS_CAST_MSG = "%s is not a %s type. CIM Type is %d and VT type is %d";

    /**
     * Private constructor so this class can't be instantiated from the outside.
     */
    private WmiUtil() {
    }

    /**
     * Translate a WmiQuery to the actual query string
     * 
     * @param <T>
     *            The properties enum
     * @param query
     *            The WmiQuery object
     * @return The string that is queried in WMI
     */
    public static <T extends Enum<T>> String queryToString(WmiQuery<T> query) {
        T[] props = query.getPropertyEnum().getEnumConstants();
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(props[0].name());
        for (int i = 1; i < props.length; i++) {
            sb.append(',').append(props[i].name());
        }
        sb.append(" FROM ").append(query.getWmiClassName());
        return sb.toString();
    }

    /**
     * Gets a String value from a WmiResult
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_STRING) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "String",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Date value from a WmiResult as a String
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getDateString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_DATETIME) {
            String date = getStr(result, property, index);
            return date.substring(0, 4) + '-' + date.substring(4, 6) + '-' + date.substring(6, 8);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "DateTime",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Reference value from a WmiResult as a String
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, an empty-string otherwise
     */
    public static <T extends Enum<T>> String getRefString(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_REFERENCE) {
            return getStr(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "Reference",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> String getStr(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return "";
        } else if (result.getVtType(property) == Variant.VT_BSTR) {
            return (String) o;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "String-mapped",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Uint64 value from a WmiResult (parsing the String). Note that
     * while the CIM type is unsigned, the return type is signed and the parsing
     * will exclude any return values above Long.MAX_VALUE.
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null and parseable as a long, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint64(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0L;
        } else if (result.getCIMType(property) == Wbemcli.CIM_UINT64 && result.getVtType(property) == Variant.VT_BSTR) {
            return ParseUtil.parseLongOrDefault((String) o, 0L);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT64",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets an UINT32 value from a WmiResult. Note that while a UINT32 CIM type
     * is unsigned, the return type is signed and requires further processing by
     * the user if unsigned values are desired.
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint32(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_UINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets an UINT32 value from a WmiResult as a long, preserving the
     * unsignedness.
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> long getUint32asLong(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_UINT32) {
            return getInt(result, property, index) & 0xFFFFFFFFL;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Sint32 value from a WmiResult. Note that while the CIM type is
     * unsigned, the return type is signed and requires further processing by
     * the user if unsigned values are desired.
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getSint32(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_SINT32) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "SINT32",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Uint16 value from a WmiResult. Note that while the CIM type is
     * unsigned, the return type is signed and requires further processing by
     * the user if unsigned values are desired.
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> int getUint16(WmiResult<T> result, T property, int index) {
        if (result.getCIMType(property) == Wbemcli.CIM_UINT16) {
            return getInt(result, property, index);
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "UINT16",
                result.getCIMType(property), result.getVtType(property)));
    }

    private static <T extends Enum<T>> int getInt(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0;
        } else if (result.getVtType(property) == Variant.VT_I4) {
            return (int) o;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "32-bit integer",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Gets a Float value from a WmiResult
     *
     * @param <T>
     *            The enum type containing the property keys
     * @param result
     *            The WmiResult from which to fetch the value
     * @param property
     *            The property (column) to fetch
     * @param index
     *            The index (row) to fetch
     * @return The stored value if non-null, 0 otherwise
     */
    public static <T extends Enum<T>> float getFloat(WmiResult<T> result, T property, int index) {
        Object o = result.getValue(property, index);
        if (o == null) {
            return 0f;
        } else if (result.getCIMType(property) == Wbemcli.CIM_REAL32 && result.getVtType(property) == Variant.VT_R4) {
            return (float) o;
        }
        throw new ClassCastException(String.format(CLASS_CAST_MSG, property.name(), "Float",
                result.getCIMType(property), result.getVtType(property)));
    }

    /**
     * Provide a shared static {@code WmiQueryHandler} instance which is used to support
     * the deprecated API in the Oshi Windows API.
     *
     * @deprecated Create a {@link WmiQueryHandler} instance.
     *            This method uses a shared static {@code WmiQueryHandler} instance.
     * @return Returns a static shared {@code WmiQueryHandler} instance.
     */
    @Deprecated
    public static WmiQueryHandler getShared() {
        return createOrGetShared();
    }

    /**
     * Create or get a shared static {@code WmiQueryHandler} instance which is used
     * to support the deprecated API in this class.
     *
     * @return Create or get a static shared {@code WmiQueryHandler} instance.
     */
    @Deprecated
    private static WmiQueryHandler createOrGetShared() {
        if (SHARED == null) {
            SHARED = new WmiQueryHandler();
        }
        return SHARED;
    }
}
