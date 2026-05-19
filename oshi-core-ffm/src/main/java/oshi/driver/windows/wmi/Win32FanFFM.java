/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Fan;
import oshi.driver.common.windows.wmi.Win32Fan.SpeedProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32FanFFM extends Win32Fan {
    private Win32FanFFM() {
    }

    public static WmiResult<SpeedProperty> querySpeed() {
        return Win32Fan.querySpeed(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
