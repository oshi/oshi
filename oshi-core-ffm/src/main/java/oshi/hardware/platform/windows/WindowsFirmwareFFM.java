/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Objects;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;
import oshi.hardware.common.platform.windows.WindowsFirmware;

/**
 * Firmware data obtained from WMI using FFM.
 */
@Immutable
final class WindowsFirmwareFFM extends WindowsFirmware {

    @Override
    protected WmiQueryExecutor getWmiQueryExecutor() {
        return Objects.requireNonNull(WmiQueryExecutorFFM.createInstance());
    }
}
