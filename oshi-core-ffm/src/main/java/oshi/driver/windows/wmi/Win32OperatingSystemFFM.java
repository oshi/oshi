/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32OperatingSystem;
import oshi.driver.common.windows.wmi.Win32OperatingSystem.OSVersionProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_OperatingSystem} using FFM.
 */
@ThreadSafe
public final class Win32OperatingSystemFFM extends Win32OperatingSystem {

    private Win32OperatingSystemFFM() {
    }

    /**
     * Queries the Operating System version.
     *
     * @return OS version, product type, build number, and related fields.
     */
    public static WmiResult<OSVersionProperty> queryOsVersion() {
        WmiQuery<OSVersionProperty> osVersionQuery = new WmiQuery<>(WIN32_OPERATING_SYSTEM, OSVersionProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(osVersionQuery);
    }
}
