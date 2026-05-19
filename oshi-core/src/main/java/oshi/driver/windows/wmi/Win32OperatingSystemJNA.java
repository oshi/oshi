/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32OperatingSystem;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32OperatingSystemJNA extends Win32OperatingSystem {
    private Win32OperatingSystemJNA() {
    }

    public static WmiResult<OSVersionProperty> queryOsVersion() {
        return Win32OperatingSystem.queryOsVersion(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
