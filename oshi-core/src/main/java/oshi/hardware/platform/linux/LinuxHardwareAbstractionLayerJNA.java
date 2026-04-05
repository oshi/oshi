/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;

/**
 * JNA-based Linux hardware abstraction layer. Extends {@link LinuxHardwareAbstractionLayer}, overriding methods as FFM
 * equivalents are migrated to {@link LinuxHardwareAbstractionLayerFFM}.
 */
@ThreadSafe
public final class LinuxHardwareAbstractionLayerJNA extends LinuxHardwareAbstractionLayer {

    @Override
    public List<PowerSource> getPowerSources() {
        return LinuxPowerSourceJNA.getPowerSources();
    }
}
