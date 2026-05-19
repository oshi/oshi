/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32BaseBoard;
import oshi.driver.common.windows.wmi.Win32BaseBoard.BaseBoardProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32BaseBoardFFM extends Win32BaseBoard {
    private Win32BaseBoardFFM() {
    }

    public static WmiResult<BaseBoardProperty> queryBaseboardInfo() {
        return Win32BaseBoard.queryBaseboardInfo(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
