/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants, property enum, and WHERE clause builder for hardware-monitor WMI Hardware data.
 * <p>
 * Open Hardware Monitor and Libre Hardware Monitor publish the same {@code Hardware} schema, each under its own
 * namespace, so the namespace argument selects which monitor is queried. Their {@code HardwareType} values differ: LHM
 * renames OHM's {@code Mainboard} to {@code Motherboard}, {@code RAM} to {@code Memory}, and {@code HDD} to
 * {@code Storage}.
 *
 * @see LhmSensor#LHM_NAMESPACE
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
        /** Hardware identifier. */
        IDENTIFIER;
    }

    /**
     * Constructor.
     */
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

    /**
     * Queries the hardware identifiers for a monitored type.
     *
     * @param h           An instantiated {@link WmiQueryExecutor}. User should have already initialized COM.
     * @param namespace   the WMI namespace to query, either {@link #OHM_NAMESPACE} or {@link LhmSensor#LHM_NAMESPACE}
     * @param typeToQuery which type to filter based on
     * @param typeName    the name of the type
     * @return WmiResult of hardware identifier properties.
     */
    public static WmiResult<IdentifierProperty> queryHwIdentifier(WmiQueryExecutor h, String namespace,
            String typeToQuery, String typeName) {
        WmiQuery<IdentifierProperty> hwIdentifierQuery = new WmiQuery<>(namespace,
                buildHardwareWmiClassNameWithWhere(typeToQuery, typeName), IdentifierProperty.class);
        return h.queryWMI(hwIdentifierQuery, false);
    }
}
