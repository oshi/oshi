/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32PhysicalMemoryFFM extends Win32PhysicalMemory {
    private Win32PhysicalMemoryFFM() {
    }

    public static WmiResult<PhysicalMemoryProperty> queryPhysicalMemory() {
        return Win32PhysicalMemory.queryPhysicalMemory(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }

    public static WmiResult<PhysicalMemoryPropertyWin8> queryPhysicalMemoryWin8() {
        return Win32PhysicalMemory
                .queryPhysicalMemoryWin8(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
