/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.platform.mac.MacGraphicsCard;
import oshi.util.platform.mac.SysctlUtilFFM;

/**
 * Graphics card info obtained by system_profiler SPDisplaysDataType.
 */
@ThreadSafe
final class MacGraphicsCardFFM extends MacGraphicsCard {

    MacGraphicsCardFFM(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    @Override
    public GpuStats createStatsSession() {
        return new MacGpuStatsFFM(IS_APPLE_SILICON, getName());
    }

    public static List<GraphicsCard> getGraphicsCards() {
        return parseGraphicsCards(MacGraphicsCardFFM::new, SysctlUtilFFM::sysctl);
    }
}
