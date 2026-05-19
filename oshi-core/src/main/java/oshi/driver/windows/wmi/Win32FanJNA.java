/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Fan;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32FanJNA extends Win32Fan {
    private Win32FanJNA() {
    }

    public static WmiResult<SpeedProperty> querySpeed() {
        return Win32Fan.querySpeed(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
