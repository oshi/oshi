/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.linux;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.PowerSource;
import oshi.hardware.UsbDevice;

/**
 * JNA-based Linux hardware abstraction layer. Extends {@link LinuxHardwareAbstractionLayer}, overriding methods as FFM
 * equivalents are migrated to {@link LinuxHardwareAbstractionLayerFFM}.
 */
@ThreadSafe
public final class LinuxHardwareAbstractionLayerJNA extends LinuxHardwareAbstractionLayer {

    @Override
    public List<UsbDevice> getUsbDevices(boolean tree) {
        return LinuxUsbDevice.getUsbDevices(tree);
    }

    @Override
    public List<PowerSource> getPowerSources() {
        return LinuxPowerSource.getPowerSources();
    }
}
