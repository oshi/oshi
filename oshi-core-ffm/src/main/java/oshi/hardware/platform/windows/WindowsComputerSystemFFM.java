/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.Objects;

import oshi.annotation.concurrent.Immutable;
import oshi.driver.common.windows.wmi.WmiQueryExecutor;
import oshi.ffm.util.platform.windows.WmiQueryExecutorFFM;
import oshi.hardware.Baseboard;
import oshi.hardware.Firmware;
import oshi.hardware.common.platform.windows.WindowsComputerSystem;

/**
 * Hardware data obtained from WMI using FFM.
 */
@Immutable
final class WindowsComputerSystemFFM extends WindowsComputerSystem {

    @Override
    protected WmiQueryExecutor getWmiQueryExecutor() {
        return Objects.requireNonNull(WmiQueryExecutorFFM.createInstance());
    }

    @Override
    public Firmware createFirmware() {
        return new WindowsFirmwareFFM();
    }

    @Override
    public Baseboard createBaseboard() {
        return new WindowsBaseboardFFM();
    }
}
