/*
 * Copyright 2020-2022 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Fan}
 */
@ThreadSafe
public final class Win32Fan {

    private static final String WIN32_FAN = "Win32_Fan";

    /**
     * Fan speed property.
     */
    public enum SpeedProperty {
        DESIREDSPEED;
    }

    private Win32Fan() {
    }

    /**
     * Queries the fan speed.
     *
     * @return Currently requested fan speed, defined in revolutions per minute, when a variable speed fan is supported.
     */
    public static WmiResult<SpeedProperty> querySpeed() {
        WmiQuery<SpeedProperty> fanQuery = new WmiQuery<>(WIN32_FAN, SpeedProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(fanQuery);
    }
}
