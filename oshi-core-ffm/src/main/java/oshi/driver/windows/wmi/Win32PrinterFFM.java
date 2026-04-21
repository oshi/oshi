/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Printer;
import oshi.driver.common.windows.wmi.Win32Printer.PrinterProperty;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_Printer} using FFM.
 */
@ThreadSafe
public final class Win32PrinterFFM extends Win32Printer {

    private Win32PrinterFFM() {
    }

    /**
     * Queries printer information.
     *
     * @return Information regarding printers
     */
    public static WmiResult<PrinterProperty> queryPrinters() {
        WmiQuery<PrinterProperty> printerQuery = new WmiQuery<>(WIN32_PRINTER, PrinterProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(printerQuery);
    }
}
