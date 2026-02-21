/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.windows.wmi.Win32Printer;
import oshi.driver.windows.wmi.Win32Printer.PrinterProperty;
import oshi.hardware.Printer;
import oshi.hardware.common.AbstractPrinter;
import oshi.util.platform.windows.WmiUtil;

/**
 * Printer data obtained from WMI
 */
@Immutable
final class WindowsPrinter extends AbstractPrinter {

    WindowsPrinter(String name, String driverName, PrinterStatus status, boolean isDefault, boolean isLocal,
            String portName) {
        super(name, driverName, status, isDefault, isLocal, portName);
    }

    /**
     * Gets printers from WMI Win32_Printer.
     *
     * @return A list of printers.
     */
    public static List<Printer> getPrinters() {
        List<Printer> printers = new ArrayList<>();
        WmiResult<PrinterProperty> result = Win32Printer.queryPrinters();

        for (int i = 0; i < result.getResultCount(); i++) {
            String name = WmiUtil.getString(result, PrinterProperty.NAME, i);
            String driverName = WmiUtil.getString(result, PrinterProperty.DRIVERNAME, i);
            int statusCode = WmiUtil.getUint16(result, PrinterProperty.PRINTERSTATUS, i);
            boolean isDefault = getBooleanValue(result, PrinterProperty.DEFAULT, i);
            boolean isLocal = getBooleanValue(result, PrinterProperty.LOCAL, i);
            String portName = WmiUtil.getString(result, PrinterProperty.PORTNAME, i);

            printers.add(new WindowsPrinter(name, driverName, parseStatus(statusCode), isDefault, isLocal, portName));
        }
        return printers;
    }

    private static boolean getBooleanValue(WmiResult<PrinterProperty> result, PrinterProperty property, int index) {
        Object o = result.getValue(property, index);
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return false;
    }

    /**
     * Parses Win32_Printer PrinterStatus codes.
     * https://learn.microsoft.com/en-us/windows/win32/cimwin32prov/win32-printer
     */
    private static PrinterStatus parseStatus(int code) {
        switch (code) {
            case 1: // Other
            case 2: // Unknown
                return PrinterStatus.UNKNOWN;
            case 3: // Idle
                return PrinterStatus.IDLE;
            case 4: // Printing
                return PrinterStatus.PRINTING;
            case 5: // Warmup
                return PrinterStatus.IDLE;
            case 6: // Stopped Printing
            case 7: // Offline
                return PrinterStatus.OFFLINE;
            default:
                return PrinterStatus.UNKNOWN;
        }
    }
}
