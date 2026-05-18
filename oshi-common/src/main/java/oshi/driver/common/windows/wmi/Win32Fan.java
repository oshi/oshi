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
        /** Desired fan speed. */
        DESIREDSPEED;
    }

    /**
     * Constructor.
     */
    protected Win32Fan() {
    }

    /**
     * Queries fan speed.
     *
     * @param h An instantiated {@link WmiQueryExecutor}.
     * @return Fan speed information.
     */
    public static WmiResult<SpeedProperty> querySpeed(WmiQueryExecutor h) {
        WmiQuery<SpeedProperty> fanQuery = new WmiQuery<>(WIN32_FAN, SpeedProperty.class);
        return h.queryWMI(fanQuery);
    }
}
