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
        VERSION, PRODUCTTYPE, BUILDNUMBER, CSDVERSION, SUITEMASK;
    }

    protected Win32OperatingSystem() {
    }
}
