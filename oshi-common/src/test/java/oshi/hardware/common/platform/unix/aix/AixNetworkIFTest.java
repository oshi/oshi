/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.hardware.common.platform.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import oshi.hardware.NetworkIF.IfOperStatus;

class AixNetworkIFTest {

    @Test
    void testQueryOperStatusMapsFlagsRead() {
        assertThat(AixNetworkIF.queryOperStatus(() -> true), is(IfOperStatus.UP));
        assertThat(AixNetworkIF.queryOperStatus(() -> false), is(IfOperStatus.DOWN));
        assertThat(AixNetworkIF.queryOperStatus(() -> {
            throw new SocketException("interface gone");
        }), is(IfOperStatus.UNKNOWN));
    }

    @Test
    void testQueryOperStatusReadsHostInterfaces() throws Exception {
        // The flag read is the JDK's on every Unix; each call reads live flags, so only the range is fixed
        for (NetworkInterface netint : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            assertThat(netint.getName(), AixNetworkIF.queryOperStatus(netint),
                    is(oneOf(IfOperStatus.UP, IfOperStatus.DOWN, IfOperStatus.UNKNOWN)));
        }
    }
}
