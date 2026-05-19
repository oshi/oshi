/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32Bios;
import oshi.driver.common.windows.wmi.Win32Bios.BiosProperty;
import oshi.driver.common.windows.wmi.Win32Bios.BiosSerialProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32BiosJNA extends Win32Bios {
    private Win32BiosJNA() {
    }

    public static WmiResult<BiosSerialProperty> querySerialNumber() {
        return Win32Bios.querySerialNumber(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }

    public static WmiResult<BiosProperty> queryBiosInfo() {
        return Win32Bios.queryBiosInfo(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
