/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.NetworkInterface;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import oshi.hardware.NetworkIF.IfOperStatus;

class AixNetworkIFTest {

    @Test
    void testQueryOperStatusFollowsInterfaceFlags() throws Exception {
        // The flag test is the JDK's on every Unix, so the host's own interfaces exercise it
        for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            IfOperStatus expected = netint.isUp() ? IfOperStatus.UP : IfOperStatus.DOWN;
            assertThat(netint.getName(), AixNetworkIF.queryOperStatus(netint), is(expected));
        }
    }
}
