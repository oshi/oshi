/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryProperty;
import oshi.driver.common.windows.wmi.Win32PhysicalMemory.PhysicalMemoryPropertyWin8;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32PhysicalMemoryJNA extends Win32PhysicalMemory {
    private Win32PhysicalMemoryJNA() {
    }

    public static WmiResult<PhysicalMemoryProperty> queryPhysicalMemory() {
        return Win32PhysicalMemory.queryPhysicalMemory(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }

    public static WmiResult<PhysicalMemoryPropertyWin8> queryPhysicalMemoryWin8() {
        return Win32PhysicalMemory
                .queryPhysicalMemoryWin8(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
