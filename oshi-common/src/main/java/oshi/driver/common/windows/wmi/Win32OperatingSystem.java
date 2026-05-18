/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_OperatingSystem}.
 */
@ThreadSafe
public class Win32OperatingSystem {

    /**
     * The WMI class name.
     */
    public static final String WIN32_OPERATING_SYSTEM = "Win32_OperatingSystem";

    /**
     * Operating System properties.
     */
    public enum OSVersionProperty {
        /** VERSION property. */
        VERSION,
        /** PRODUCTTYPE property. */
        PRODUCTTYPE,
        /** BUILDNUMBER property. */
        BUILDNUMBER,
        /** CSDVERSION property. */
        CSDVERSION,
        /** SUITEMASK property. */
        SUITEMASK;
    }

    /**
     * Constructor.
     */
    protected Win32OperatingSystem() {
    }

    /**
     * Queries the operating system version.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return OS version information.
     */
    public static WmiResult<OSVersionProperty> queryOsVersion(WmiQueryExecutor h) {
        WmiQuery<OSVersionProperty> osVersionQuery = new WmiQuery<>(WIN32_OPERATING_SYSTEM, OSVersionProperty.class);
        return h.queryWMI(osVersionQuery);
    }
}
