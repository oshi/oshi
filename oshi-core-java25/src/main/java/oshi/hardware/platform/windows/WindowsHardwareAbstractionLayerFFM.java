/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.windows;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;

/**
 * FFM-based hardware abstraction layer for Windows. Extends {@link WindowsHardwareAbstractionLayer}, overriding methods
 * as FFM implementations become available.
 */
@ThreadSafe
public final class WindowsHardwareAbstractionLayerFFM extends WindowsHardwareAbstractionLayer {

    @Override
    public List<PowerSource> getPowerSources() {
        return WindowsPowerSourceFFM.getPowerSources();
    }
}
