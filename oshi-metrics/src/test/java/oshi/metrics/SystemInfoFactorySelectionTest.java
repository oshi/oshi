/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

/**
 * Validates that SystemInfoFactory selects the correct provider based on JDK version when both oshi-core and
 * oshi-core-ffm are on the classpath.
 */
class SystemInfoFactorySelectionTest {

    @Test
    void factorySelectsCorrectProvider() {
        SystemInfoProvider provider = SystemInfoFactory.create();
        if (Runtime.version().feature() >= 25) {
            assertEquals("oshi.ffm.SystemInfo", provider.getClass().getName(),
                    "JDK 25+: Factory should select FFM provider");
            assertEquals(20, provider.getPriority());
        } else {
            assertEquals("oshi.SystemInfo", provider.getClass().getName(),
                    "JDK < 25: Factory should select JNA provider");
            assertEquals(10, provider.getPriority());
        }
    }
}
