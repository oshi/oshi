/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

/**
 * Common interface for WMI query results, abstracting JNA and FFM implementations.
 *
 * @param <T> the enum type representing the queried properties
 */
public interface WmiResult<T extends Enum<T>> {

    /**
     * Gets the number of results in this result set.
     *
     * @return the result count
     */
    int getResultCount();

    /**
     * Gets the value of a property at the given index.
     *
     * @param property the property enum constant
     * @param index    the row index
     * @return the value
     */
    Object getValue(T property, int index);

    /**
     * Gets the variant type of a property.
     *
     * @param property the property enum constant
     * @return the VT type constant
     */
    int getVtType(T property);

    /**
     * Gets the CIM type of a property.
     *
     * @param property the property enum constant
     * @return the CIM type constant
     */
    int getCIMType(T property);
}
