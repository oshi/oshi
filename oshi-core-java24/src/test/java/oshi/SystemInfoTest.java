/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SystemInfoTest {

    @Test
    public void testGetCurrentPlatform() {
        PlatformEnum platform = SystemInfo.getCurrentPlatform();
        assertNotNull(platform, "Platform should not be null");
        assertNotEquals(PlatformEnum.UNSUPPORTED, platform, "Platform should be recognized");
    }
}
