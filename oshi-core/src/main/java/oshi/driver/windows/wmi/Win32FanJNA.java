/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Fan;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_Fan} using JNA.
 */
@ThreadSafe
public final class Win32FanJNA extends Win32Fan {

    private Win32FanJNA() {
    }

    /**
     * Queries the fan speed.
     *
     * @return Currently requested fan speed, defined in revolutions per minute, when a variable speed fan is supported.
     */
    public static WmiResult<SpeedProperty> querySpeed() {
        WmiQuery<SpeedProperty> fanQuery = new WmiQuery<>(WIN32_FAN, SpeedProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(fanQuery);
    }
}
