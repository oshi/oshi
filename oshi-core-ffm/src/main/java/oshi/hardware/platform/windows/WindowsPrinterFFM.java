/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.ArrayList;
import java.util.List;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.Win32Printer.PrinterProperty;
import oshi.driver.windows.wmi.Win32PrinterFFM;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiUtilFFM;
import oshi.hardware.Printer;
import oshi.hardware.common.platform.windows.WindowsPrinter;

/**
 * Printer data obtained from WMI using FFM.
 */
@Immutable
final class WindowsPrinterFFM extends WindowsPrinter {

    WindowsPrinterFFM(String name, String driverName, String description, PrinterStatus status, String statusReason,
            boolean isDefault, boolean isLocal, String portName) {
        super(name, driverName, description, status, statusReason, isDefault, isLocal, portName);
    }

    /**
     * Gets printers from WMI Win32_Printer.
     *
     * @return A list of printers.
     */
    public static List<Printer> getPrinters() {
        List<Printer> printers = new ArrayList<>();
        WmiResult<PrinterProperty> result = Win32PrinterFFM.queryPrinters();

        for (int i = 0; i < result.getResultCount(); i++) {
            String name = WmiUtilFFM.getString(result, PrinterProperty.NAME, i);
            String driverName = WmiUtilFFM.getString(result, PrinterProperty.DRIVERNAME, i);
            String description = WmiUtilFFM.getString(result, PrinterProperty.DESCRIPTION, i);
            int statusCode = WmiUtilFFM.getUint16(result, PrinterProperty.PRINTERSTATUS, i);
            int errorState = WmiUtilFFM.getUint16(result, PrinterProperty.DETECTEDERRORSTATE, i);
            boolean isDefault = getBooleanValue(result, PrinterProperty.DEFAULT, i);
            boolean isLocal = getBooleanValue(result, PrinterProperty.LOCAL, i);
            String portName = WmiUtilFFM.getString(result, PrinterProperty.PORTNAME, i);

            printers.add(new WindowsPrinterFFM(name, driverName, description, parseStatus(statusCode, errorState),
                    parseErrorState(errorState), isDefault, isLocal, portName));
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
}
