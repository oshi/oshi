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
        /** NAME property. */
        NAME,
        /** DRIVERNAME property. */
        DRIVERNAME,
        /** PRINTERSTATUS property. */
        PRINTERSTATUS,
        /** DETECTEDERRORSTATE property. */
        DETECTEDERRORSTATE,
        /** DEFAULT property. */
        DEFAULT,
        /** LOCAL property. */
        LOCAL,
        /** PORTNAME property. */
        PORTNAME,
        /** DESCRIPTION property. */
        DESCRIPTION;
    }

    /**
     * Constructor.
     */
    protected Win32Printer() {
    }

    /**
     * Queries printer information.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Printer information.
     */
    public static WmiResult<PrinterProperty> queryPrinters(WmiQueryExecutor h) {
        WmiQuery<PrinterProperty> query = new WmiQuery<>(WIN32_PRINTER, PrinterProperty.class);
        return h.queryWMI(query);
    }
}
