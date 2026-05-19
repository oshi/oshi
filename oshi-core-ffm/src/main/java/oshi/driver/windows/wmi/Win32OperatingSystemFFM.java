/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32OperatingSystem;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32OperatingSystemFFM extends Win32OperatingSystem {
    private Win32OperatingSystemFFM() {
    }

    public static WmiResult<OSVersionProperty> queryOsVersion() {
        return Win32OperatingSystem.queryOsVersion(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
