/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.platform.mac;

import java.util.List;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.hardware.GpuStats;
import oshi.hardware.GraphicsCard;
import oshi.hardware.common.platform.mac.MacGraphicsCard;
import oshi.util.platform.mac.SysctlUtil;

/**
 * Graphics card info obtained by system_profiler SPDisplaysDataType using JNA.
 */
@ThreadSafe
final class MacGraphicsCardJNA extends MacGraphicsCard {

    MacGraphicsCardJNA(String name, String deviceId, String vendor, String versionInfo, long vram) {
        super(name, deviceId, vendor, versionInfo, vram);
    }

    @Override
    public GpuStats createStatsSession() {
        return new MacGpuStats(IS_APPLE_SILICON, getName());
    }

    public static List<GraphicsCard> getGraphicsCards() {
        return parseGraphicsCards(MacGraphicsCardJNA::new, SysctlUtil::sysctl);
    }
}
