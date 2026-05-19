/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32ComputerSystem;
import oshi.driver.common.windows.wmi.Win32ComputerSystem.ComputerSystemProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32ComputerSystemJNA extends Win32ComputerSystem {
    private Win32ComputerSystemJNA() {
    }

    public static WmiResult<ComputerSystemProperty> queryComputerSystem() {
        return Win32ComputerSystem.queryComputerSystem(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
