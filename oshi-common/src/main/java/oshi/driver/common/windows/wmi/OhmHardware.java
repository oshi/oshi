/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants, property enum, and WHERE clause builder for Open Hardware Monitor WMI Hardware data.
 */
@ThreadSafe
public class OhmHardware {

    /**
     * The WMI namespace for Open Hardware Monitor.
     */
    public static final String OHM_NAMESPACE = "ROOT\\OpenHardwareMonitor";

    /**
     * The WMI class name for hardware.
     */
    public static final String HARDWARE = "Hardware";

    /**
     * HW Identifier Property.
     */
    public enum IdentifierProperty {
        IDENTIFIER;
    }

    protected OhmHardware() {
    }

    /**
     * Builds the WMI class name with WHERE clause for hardware identifier queries.
     *
     * @param typeToQuery which type to filter based on
     * @param typeName    the name of the type
     * @return the WMI class name with WHERE clause
     */
    public static String buildHardwareWmiClassNameWithWhere(String typeToQuery, String typeName) {
        StringBuilder sb = new StringBuilder(HARDWARE);
        sb.append(" WHERE ").append(typeToQuery).append("Type=\"").append(typeName).append('"');
        return sb.toString();
    }
}
