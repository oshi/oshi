/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Printer;
import oshi.driver.common.windows.wmi.Win32Printer.PrinterProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32PrinterJNA extends Win32Printer {
    private Win32PrinterJNA() {
    }

    public static WmiResult<PrinterProperty> queryPrinters() {
        return Win32Printer.queryPrinters(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
