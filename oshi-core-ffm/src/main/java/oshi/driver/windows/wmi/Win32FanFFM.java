/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Fan;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_Fan} using FFM.
 */
@ThreadSafe
public final class Win32FanFFM extends Win32Fan {

    private Win32FanFFM() {
    }

    /**
     * Queries the fan speed.
     *
     * @return Currently requested fan speed, defined in revolutions per minute, when a variable speed fan is supported.
     */
    public static WmiResult<SpeedProperty> querySpeed() {
        WmiQuery<SpeedProperty> fanQuery = new WmiQuery<>(WIN32_FAN, SpeedProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(fanQuery);
    }
}
