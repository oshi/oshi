/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.ComputerSystem;
import oshi.hardware.PowerSource;
import oshi.hardware.Printer;

/**
 * FFM-based hardware abstraction layer for Windows. Extends {@link WindowsHardwareAbstractionLayer}, overriding methods
 * as FFM implementations become available.
 */
@ThreadSafe
public final class WindowsHardwareAbstractionLayerFFM extends WindowsHardwareAbstractionLayer {

    @Override
    public ComputerSystem createComputerSystem() {
        return new WindowsComputerSystemFFM();
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSourceFFM.getPowerSources();
    }

    @Override
    public List<Printer> getPrinters() {
        return WindowsPrinterFFM.getPrinters();
    }
}
