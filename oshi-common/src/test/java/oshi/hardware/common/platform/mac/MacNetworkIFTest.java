/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.hardware.NetworkIF.IfOperStatus;

class MacNetworkIFTest {

    private static final int UP_RUNNING = 0x8863;
    private static final int NOT_UP = 0x8822;
    private static final int UP_NOT_RUNNING = 0x8823;

    @Test
    void testOperStatus() {
        // Ethernet or Wi-Fi with media: the link decides, since macOS sets RUNNING on every up interface
        assertThat(MacNetworkIF.operStatus(UP_RUNNING, 0x3), is(IfOperStatus.UP));
        assertThat(MacNetworkIF.operStatus(UP_RUNNING, 0x1), is(IfOperStatus.DOWN));
        // Administratively down
        assertThat(MacNetworkIF.operStatus(NOT_UP, 0x3), is(IfOperStatus.DOWN));
        // No media reported (loopback, tunnels): the flags decide
        assertThat(MacNetworkIF.operStatus(UP_RUNNING, -1), is(IfOperStatus.UP));
        assertThat(MacNetworkIF.operStatus(UP_RUNNING, 0), is(IfOperStatus.UP));
        assertThat(MacNetworkIF.operStatus(UP_NOT_RUNNING, -1), is(IfOperStatus.DOWN));
    }
}
