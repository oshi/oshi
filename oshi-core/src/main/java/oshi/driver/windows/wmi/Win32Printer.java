/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Printer}
 */
@ThreadSafe
public final class Win32Printer {

    private static final String WIN32_PRINTER = "Win32_Printer";

    /**
     * Printer properties from WMI
     */
    public enum PrinterProperty {
        NAME, DRIVERNAME, PRINTERSTATUS, DEFAULT, LOCAL, PORTNAME;
    }

    private Win32Printer() {
    }

    /**
     * Queries printer information.
     *
     * @return Information regarding printers
     */
    public static WmiResult<PrinterProperty> queryPrinters() {
        WmiQuery<PrinterProperty> printerQuery = new WmiQuery<>(WIN32_PRINTER, PrinterProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(printerQuery);
    }
}
