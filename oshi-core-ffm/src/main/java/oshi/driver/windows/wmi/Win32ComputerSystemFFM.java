/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystem;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32ComputerSystemFFM extends Win32ComputerSystem {
    private Win32ComputerSystemFFM() {
    }

    public static WmiResult<ComputerSystemProperty> queryComputerSystem() {
        return Win32ComputerSystem.queryComputerSystem(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
