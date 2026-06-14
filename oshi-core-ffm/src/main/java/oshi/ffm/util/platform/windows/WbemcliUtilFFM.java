/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.ffm.util.platform.windows;

import static oshi.ffm.platform.windows.com.VariantFFM.VT_BOOL;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_BSTR;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_EMPTY;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_I1;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_I2;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_I4;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_I8;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_INT;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_NULL;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_R4;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_R8;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_UI1;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_UI2;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_UI4;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_UI8;
import static oshi.ffm.platform.windows.com.VariantFFM.VT_UINT;
import static oshi.ffm.platform.windows.com.VariantFFM.clear;
import static oshi.ffm.platform.windows.com.VariantFFM.getBoolVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getBstrVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getByteVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getDoubleVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getFloatVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getIntVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getLongVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getShortVal;
import static oshi.ffm.platform.windows.com.VariantFFM.getVt;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.ffm.platform.windows.com.IWbemClassObjectFFM;
import oshi.ffm.platform.windows.com.WbemcliFFM;

/**
 * FFM-based utility providing WMI query and result classes that mirror the JNA WbemcliUtil API.
 */
public final class WbemcliUtilFFM {

    private static final Logger LOG = LoggerFactory.getLogger(WbemcliUtilFFM.class);

    private WbemcliUtilFFM() {
    }

    /**
     * Helper class wrapping information required for a WMI query.
     *
     * @param <T> An enum whose constants define the WMI properties to query
     */
    public static class WmiQuery<T extends Enum<T>> {

        private String nameSpace;
        private final String wmiClassName;
        private final Class<T> propertyEnum;

        /**
         * Instantiate a WmiQuery in the default namespace.
         *
         * @param wmiClassName The WMI Class to use. May include a WHERE clause.
         * @param propertyEnum An Enum that contains the properties to query
         */
        public WmiQuery(String wmiClassName, Class<T> propertyEnum) {
            this(WbemcliFFM.DEFAULT_NAMESPACE, wmiClassName, propertyEnum);
        }

        /**
         * Instantiate a WmiQuery.
         *
         * @param nameSpace    The WMI namespace to use
         * @param wmiClassName The WMI class to use. May include a WHERE clause.
         * @param propertyEnum An enum for type mapping
         */
        public WmiQuery(String nameSpace, String wmiClassName, Class<T> propertyEnum) {
            this.nameSpace = nameSpace;
            this.wmiClassName = wmiClassName;
            this.propertyEnum = propertyEnum;
        }

        /**
         * Gets the property enum class.
         *
         * @return the property enum class
         */
        public Class<T> getPropertyEnum() {
            return propertyEnum;
        }

        /**
         * Gets the WMI namespace.
         *
         * @return the namespace
         */
        public String getNameSpace() {
            return nameSpace;
        }

        /**
         * Sets the WMI namespace.
         *
         * @param nameSpace the namespace to set
         */
        public void setNameSpace(String nameSpace) {
            this.nameSpace = nameSpace;
        }

        /**
         * Gets the WMI class name.
         *
         * @return the WMI class name
         */
        public String getWmiClassName() {
            return wmiClassName;
        }
    }

    /**
     * Helper class wrapping the results of a WMI query, indexed by enum property and row.
     *
     * @param <T> An enum whose constants define the WMI properties
     */
    public static class WmiResult<T extends Enum<T>> {

        private final Map<T, List<Object>> propertyMap;
        private final Map<T, Integer> vtTypeMap;
        private final Map<T, Integer> cimTypeMap;
        private int resultCount;

        /**
         * Instantiate a WmiResult for the given property enum.
         *
         * @param propertyEnum the enum class defining WMI properties
         */
        public WmiResult(Class<T> propertyEnum) {
            propertyMap = new EnumMap<>(propertyEnum);
            vtTypeMap = new EnumMap<>(propertyEnum);
            cimTypeMap = new EnumMap<>(propertyEnum);
            for (T prop : propertyEnum.getEnumConstants()) {
                propertyMap.put(prop, new ArrayList<>());
                vtTypeMap.put(prop, 0); // VT_EMPTY
                cimTypeMap.put(prop, WbemcliFFM.CIM_EMPTY);
            }
        }

        /**
         * Gets the value for a property at the specified row index.
         *
         * @param property the property enum constant
         * @param index    the row index
         * @return the value object
         */
        public Object getValue(T property, int index) {
            return propertyMap.get(property).get(index);
        }

        /**
         * Gets the VARIANT type for a property.
         *
         * @param property the property enum constant
         * @return the VT type code
         */
        public int getVtType(T property) {
            return vtTypeMap.get(property);
        }

        /**
         * Gets the CIM type for a property.
         *
         * @param property the property enum constant
         * @return the CIM type code
         */
        public int getCIMType(T property) {
            return cimTypeMap.get(property);
        }

        /**
         * Gets the number of result rows.
         *
         * @return the result count
         */
        public int getResultCount() {
            return resultCount;
        }

        /**
         * Adds a value for the given property.
         *
         * @param vtType   the VT type
         * @param cimType  the CIM type
         * @param property the property enum constant
         * @param value    the value (may be null)
         */
        public void add(int vtType, int cimType, T property, Object value) {
            propertyMap.get(property).add(value);
            if (vtType != VT_NULL && vtType != VT_EMPTY && vtTypeMap.get(property) == 0) {
                vtTypeMap.put(property, vtType);
            }
            if (cimType != WbemcliFFM.CIM_EMPTY && cimTypeMap.get(property) == WbemcliFFM.CIM_EMPTY) {
                cimTypeMap.put(property, cimType);
            }
        }

        /**
         * Increments the result count.
         */
        public void incrementResultCount() {
            resultCount++;
        }
    }

    /**
     * Populates a WmiResult row from an IWbemClassObject by reading each property defined in the enum.
     *
     * @param <T>     the property enum type
     * @param pObject the IWbemClassObject pointer
     * @param arena   the arena for memory allocation
     * @param result  the WmiResult to populate
     */
    public static <T extends Enum<T>> void populateRow(MemorySegment pObject, Arena arena, WmiResult<T> result) {
        for (T property : result.cimTypeMap.keySet()) {
            IWbemClassObjectFFM.GetResult getResult = IWbemClassObjectFFM.get(pObject, property.name(), arena);
            try {
                if (!getResult.succeeded()) {
                    result.add(0, WbemcliFFM.CIM_EMPTY, property, null);
                    continue;
                }
                int vt = getVt(getResult.variant());
                int cimType = getResult.cimType();
                switch (vt) {
                    case VT_BSTR:
                        result.add(vt, cimType, property, getBstrVal(getResult.variant(), arena));
                        break;
                    case VT_I4, VT_INT, VT_UI4, VT_UINT:
                        result.add(vt, cimType, property, getIntVal(getResult.variant()));
                        break;
                    case VT_I8, VT_UI8:
                        result.add(vt, cimType, property, getLongVal(getResult.variant()));
                        break;
                    case VT_I2:
                        result.add(vt, cimType, property, (int) getShortVal(getResult.variant()));
                        break;
                    case VT_UI2:
                        result.add(vt, cimType, property, getShortVal(getResult.variant()) & 0xFFFF);
                        break;
                    case VT_I1:
                        result.add(vt, cimType, property, (int) getByteVal(getResult.variant()));
                        break;
                    case VT_UI1:
                        result.add(vt, cimType, property, getByteVal(getResult.variant()) & 0xFF);
                        break;
                    case VT_BOOL:
                        result.add(vt, cimType, property, getBoolVal(getResult.variant()));
                        break;
                    case VT_R4:
                        result.add(vt, cimType, property, getFloatVal(getResult.variant()));
                        break;
                    case VT_R8:
                        result.add(vt, cimType, property, getDoubleVal(getResult.variant()));
                        break;
                    case VT_NULL, VT_EMPTY:
                        result.add(vt, cimType, property, null);
                        break;
                    default:
                        LOG.debug("Unhandled VT type {} for property {}", vt, property.name());
                        result.add(VT_EMPTY, cimType, property, null);
                        break;
                }
            } finally {
                clear(getResult.variant());
            }
        }
        result.incrementResultCount();
    }
}
