/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import java.util.Objects;

/**
 * Encapsulates a WMI query with a namespace, class name, and property enum.
 *
 * @param <T> the enum type representing the properties to query
 */
public class WmiQuery<T extends Enum<T>> {

    private final String nameSpace;
    private final String wmiClassName;
    private final Class<T> propertyEnum;

    /**
     * Creates a WMI query for the default namespace ({@code ROOT\CIMV2}).
     *
     * @param wmiClassName the WMI class name
     * @param propertyEnum the property enum class
     */
    public WmiQuery(String wmiClassName, Class<T> propertyEnum) {
        this("ROOT\\CIMV2", wmiClassName, propertyEnum);
    }

    /**
     * Creates a WMI query for a specific namespace.
     *
     * @param nameSpace    the WMI namespace
     * @param wmiClassName the WMI class name
     * @param propertyEnum the property enum class
     */
    public WmiQuery(String nameSpace, String wmiClassName, Class<T> propertyEnum) {
        this.nameSpace = Objects.requireNonNull(nameSpace, "nameSpace");
        this.wmiClassName = Objects.requireNonNull(wmiClassName, "wmiClassName");
        this.propertyEnum = Objects.requireNonNull(propertyEnum, "propertyEnum");
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
     * Gets the WMI class name.
     *
     * @return the class name
     */
    public String getWmiClassName() {
        return wmiClassName;
    }

    /**
     * Gets the property enum class.
     *
     * @return the property enum class
     */
    public Class<T> getPropertyEnum() {
        return propertyEnum;
    }
}
