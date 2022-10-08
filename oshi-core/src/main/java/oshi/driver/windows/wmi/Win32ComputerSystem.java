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
 * Utility to query WMI class {@code Win32_ComputerSystem}
 */
@ThreadSafe
public final class Win32ComputerSystem {

    private static final String WIN32_COMPUTER_SYSTEM = "Win32_ComputerSystem";

    /**
     * Computer System properties
     */
    public enum ComputerSystemProperty {
        MANUFACTURER, MODEL;
    }

    private Win32ComputerSystem() {
    }

    /**
     * Queries the Computer System.
     *
     * @return Computer System Manufacturer and Model
     */
    public static WmiResult<ComputerSystemProperty> queryComputerSystem() {
        WmiQuery<ComputerSystemProperty> computerSystemQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM,
                ComputerSystemProperty.class);
        return Objects.requireNonNull(WmiQueryHandler.createInstance()).queryWMI(computerSystemQuery);
    }
}
