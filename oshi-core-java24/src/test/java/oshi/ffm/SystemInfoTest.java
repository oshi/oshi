/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import oshi.ffm.PlatformEnum;
import oshi.ffm.SystemInfo;
import org.junit.jupiter.api.Test;
import oshi.ffm.hardware.HardwareAbstractionLayer;
import oshi.ffm.software.os.OperatingSystem;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemInfoTest {

    @Test
    public void testGetCurrentPlatform() {
        PlatformEnum platform = SystemInfo.getCurrentPlatform();
        assertNotNull(platform, "Platform should not be null");
        assertNotEquals(PlatformEnum.UNSUPPORTED, platform, "Platform should be recognized");
    }

    @Test
    void testGetOperatingSystem() {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os);
        assertNotNull(os.toString()); // Optional: check delegate works
    }

    @Test
    void testGetHardware() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();
        assertNotNull(hw);
    }
}
