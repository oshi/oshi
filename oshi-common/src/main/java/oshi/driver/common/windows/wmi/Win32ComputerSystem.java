/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_ComputerSystem}.
 */
@ThreadSafe
public class Win32ComputerSystem {

    /**
     * The WMI class name.
     */
    public static final String WIN32_COMPUTER_SYSTEM = "Win32_ComputerSystem";

    /**
     * Computer System properties.
     */
    public enum ComputerSystemProperty {
        MANUFACTURER, MODEL;
    }

    protected Win32ComputerSystem() {
    }
}
