/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UnixDisplay}.
 */
class UnixDisplayTest {

    @Test
    void testDevicePortDefaultsToUnknown() {
        assertThat(new UnixDisplay(new byte[128]).getDevicePort(), is("unknown"));
    }

    @Test
    void testDevicePortFromConnectorConstructor() {
        assertThat(new UnixDisplay(new byte[128], "HDMI-A-1", 96).getDevicePort(), is("HDMI-A-1"));
    }
}
