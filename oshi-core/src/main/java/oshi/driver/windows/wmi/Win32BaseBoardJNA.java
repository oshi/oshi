/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32BaseBoard;
import oshi.driver.common.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32BaseBoardJNA extends Win32BaseBoard {
    private Win32BaseBoardJNA() {
    }

    public static WmiResult<BaseBoardProperty> queryBaseboardInfo() {
        return Win32BaseBoard.queryBaseboardInfo(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
