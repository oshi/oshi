/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Objects;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.hardware.common.platform.windows.WindowsBaseboard;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

/**
 * Baseboard data obtained from WMI using JNA.
 */
@Immutable
final class WindowsBaseboardJNA extends WindowsBaseboard {

    @Override
    protected WmiQueryExecutor getWmiQueryExecutor() {
        return Objects.requireNonNull(WmiQueryExecutorJNA.createInstance());
    }
}
