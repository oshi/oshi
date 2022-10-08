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
 * Utility to query WMI class {@code Win32_OperatingSystem}
 */
@ThreadSafe
public final class Win32OperatingSystem {

    private static final String WIN32_OPERATING_SYSTEM = "Win32_OperatingSystem";

    /**
     * Operating System properties
     */
    public enum OSVersionProperty {
        VERSION, PRODUCTTYPE, BUILDNUMBER, CSDVERSION, SUITEMASK;
    }

    private Win32OperatingSystem() {
    }

    /**
     * Queries the Computer System.
     *
     * @return Computer System Manufacturer and Model
     */
    public static WmiResult<OSVersionProperty> queryOsVersion() {
        WmiQuery<OSVersionProperty> osVersionQuery = new WmiQuery<>(WIN32_OPERATING_SYSTEM, OSVersionProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(osVersionQuery);
    }
}
