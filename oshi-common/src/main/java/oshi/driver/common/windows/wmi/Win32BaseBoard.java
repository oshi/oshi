/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_BaseBoard}.
 */
@ThreadSafe
public class Win32BaseBoard {

    /**
     * The WMI class name.
     */
    public static final String WIN32_BASEBOARD = "Win32_BaseBoard";

    /**
     * Baseboard description properties.
     */
    public enum BaseBoardProperty {
        MANUFACTURER, MODEL, PRODUCT, VERSION, SERIALNUMBER;
    }

    protected Win32BaseBoard() {
    }
}
