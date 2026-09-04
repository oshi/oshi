/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import org.jspecify.annotations.Nullable;

/**
 * Common interface for WMI query results, abstracting JNA and FFM implementations.
 *
 * @param <T> the enum type representing the queried properties
 */
public interface WmiResult<T extends Enum<T>> {

    /**
     * Returns a result with no rows, for a query which was not executed.
     *
     * @param <T> the enum type representing the queried properties
     * @return an empty result
     */
    static <T extends Enum<T>> WmiResult<T> empty() {
        return new WmiResult<T>() {
            @Override
            public int getResultCount() {
                return 0;
            }

            @Override
            public @Nullable Object getValue(T property, int index) {
                return null;
            }

            @Override
            public int getVtType(T property) {
                return WmiConstants.VT_EMPTY;
            }

            @Override
            public int getCIMType(T property) {
                return WmiConstants.CIM_EMPTY;
            }
        };
    }

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
     * @return the value, or {@code null} if the property was not set on this row
     */
    @Nullable
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
