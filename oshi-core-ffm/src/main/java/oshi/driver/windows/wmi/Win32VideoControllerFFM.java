/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32VideoController;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;

@ThreadSafe
public final class Win32VideoControllerFFM extends Win32VideoController {
    private Win32VideoControllerFFM() {
    }

    public static WmiResult<VideoControllerProperty> queryVideoController() {
        return Win32VideoController.queryVideoController(Objects.requireNonNull(WmiQueryExecutorFFM.createInstance()));
    }
}
