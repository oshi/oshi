/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;

import oshi.SystemInfoFFM;
import oshi.software.os.mac.MacOperatingSystemFFM;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test OS
 */
@EnabledForJreRange(min = JRE.JAVA_24)
@TestInstance(Lifecycle.PER_CLASS)
public class OperatingSystemFFMTest extends OperatingSystemTest {
    @Override
    protected OperatingSystem createOperatingSystem() {
        return new SystemInfoFFM().getOperatingSystem();
    }

    @EnabledOnOs(OS.MAC)
    @Test
    void testGetMacOperatingSystem() {
        assertTrue(os instanceof MacOperatingSystemFFM);
    }
}
