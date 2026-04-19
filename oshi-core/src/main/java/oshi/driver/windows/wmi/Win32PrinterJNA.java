/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Printer;
import oshi.driver.common.windows.wmi.Win32Printer.PrinterProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Printer} using JNA.
 */
@ThreadSafe
public final class Win32PrinterJNA extends Win32Printer {

    private Win32PrinterJNA() {
    }

    /**
     * Queries printer information.
     *
     * @return Information regarding printers
     */
    public static WmiResult<PrinterProperty> queryPrinters() {
        WmiQuery<PrinterProperty> printerQuery = new WmiQuery<>(WIN32_PRINTER, PrinterProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(printerQuery);
    }
}
