/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.driver.windows.wmi;

import java.util.Objects;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.common.windows.wmi.Win32VideoController;
import oshi.driver.common.windows.wmi.Win32VideoController.VideoControllerProperty;
import oshi.driver.common.windows.wmi.WmiResult;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

@ThreadSafe
public final class Win32VideoControllerJNA extends Win32VideoController {
    private Win32VideoControllerJNA() {
    }

    public static WmiResult<VideoControllerProperty> queryVideoController() {
        return Win32VideoController.queryVideoController(Objects.requireNonNull(WmiQueryExecutorJNA.createInstance()));
    }
}
