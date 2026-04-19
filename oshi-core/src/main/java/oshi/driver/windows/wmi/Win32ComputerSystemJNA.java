/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystem;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.util.platform.windows.WmiQueryHandler;

/**
 * Utility to query WMI class {@code Win32_ComputerSystem} using JNA.
 */
@ThreadSafe
public final class Win32ComputerSystemJNA extends Win32ComputerSystem {

    private Win32ComputerSystemJNA() {
    }

    /**
     * Queries the Computer System.
     *
     * @return Computer System Manufacturer and Model
     */
    public static WmiResult<ComputerSystemProperty> queryComputerSystem() {
        WmiQuery<ComputerSystemProperty> computerSystemQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM,
                ComputerSystemProperty.class);
        return Objects
                .requireNonNull(WmiQueryHandler.createInstance(), "WmiQueryHandler.createInstance() returned null")
                .queryWMI(computerSystemQuery);
    }
}
