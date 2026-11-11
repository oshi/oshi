/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.windows;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class WindowsPowerSourceTest {

    @Test
    void testBatteryName() {
        assertThat(WindowsPowerSource.batteryName(0), is("System Battery"));
        assertThat(WindowsPowerSource.batteryName(1), is("System Battery 2"));
    }
}
