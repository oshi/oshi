/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystem;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiQuery;
import oshi.ffm.util.platform.windows.WbemcliUtilFFM.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryHandlerFFM;

/**
 * Utility to query WMI class {@code Win32_ComputerSystem} using FFM.
 */
@ThreadSafe
public final class Win32ComputerSystemFFM extends Win32ComputerSystem {

    private Win32ComputerSystemFFM() {
    }

    /**
     * Queries the Computer System.
     *
     * @return Computer System Manufacturer and Model
     */
    public static WmiResult<ComputerSystemProperty> queryComputerSystem() {
        WmiQuery<ComputerSystemProperty> computerSystemQuery = new WmiQuery<>(WIN32_COMPUTER_SYSTEM,
                ComputerSystemProperty.class);
        return Objects.requireNonNull(WmiQueryHandlerFFM.createInstance(),
                "WmiQueryHandlerFFM.createInstance() returned null").queryWMI(computerSystemQuery);
    }
}
