/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.software.os.mac.MacOperatingSystemFFM;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @EnabledOnOs(OS.MAC)
    @Test
    void testGetMacOperatingSystem() {
        SystemInfoFFM si = new SystemInfoFFM();
        OperatingSystem os = si.getOperatingSystem();
        assertTrue(os instanceof MacOperatingSystemFFM);
    }

    @Test
    void testGetHardware() {
        SystemInfoFFM si = new SystemInfoFFM();
        HardwareAbstractionLayer hw = si.getHardware();
        assertNotNull(hw);
    }
}
