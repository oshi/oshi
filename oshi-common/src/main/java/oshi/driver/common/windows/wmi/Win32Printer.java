/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_Printer}.
 */
@ThreadSafe
public class Win32Printer {

    /**
     * The WMI class name.
     */
    public static final String WIN32_PRINTER = "Win32_Printer";

    /**
     * Printer properties from WMI.
     */
    public enum PrinterProperty {
        NAME, DRIVERNAME, PRINTERSTATUS, DETECTEDERRORSTATE, DEFAULT, LOCAL, PORTNAME, DESCRIPTION;
    }

    protected Win32Printer() {
    }
}
