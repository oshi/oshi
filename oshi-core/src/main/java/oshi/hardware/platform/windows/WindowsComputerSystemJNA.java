/*
 * Copyright 2016-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Objects;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.platform.windows.WindowsComputerSystem;
import oshi.util.platform.windows.WmiQueryExecutorJNA;

/**
 * Hardware data obtained from WMI using JNA.
 */
@Immutable
final class WindowsComputerSystemJNA extends WindowsComputerSystem {

    @Override
    protected WmiQueryExecutor getWmiQueryExecutor() {
        return Objects.requireNonNull(WmiQueryExecutorJNA.createInstance());
    }

    @Override
    public Firmware createFirmware() {
        return new WindowsFirmwareJNA();
    }

    @Override
    public Baseboard createBaseboard() {
        return new WindowsBaseboardJNA();
    }
}
