/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import org.junit.jupiter.api.Test;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemInfoFFMTest {

    @Test
    public void testGetCurrentPlatform() {
        PlatformEnumFFM platform = SystemInfoFFM.getCurrentPlatform();
        assertNotNull(platform, "Platform should not be null");
        assertNotEquals(PlatformEnumFFM.UNSUPPORTED, platform, "Platform should be recognized");
    }

    @Test
    void testGetOperatingSystem() {
        SystemInfoFFM si = new SystemInfoFFM();
        OperatingSystem os = si.getOperatingSystem();
        assertNotNull(os);
    }

    @Test
    void testGetHardware() {
        SystemInfoFFM si = new SystemInfoFFM();
        HardwareAbstractionLayer hw = si.getHardware();
        assertNotNull(hw);
    }
}
