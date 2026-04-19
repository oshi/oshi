/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.common.windows.wmi;

import oshi.annotation.concurrent.ThreadSafe;

/**
 * Constants and property enum for WMI class {@code Win32_Fan}.
 */
@ThreadSafe
public class Win32Fan {

    /**
     * The WMI class name.
     */
    public static final String WIN32_FAN = "Win32_Fan";

    /**
     * Fan speed property.
     */
    public enum SpeedProperty {
        DESIREDSPEED;
    }

    protected Win32Fan() {
    }
}
