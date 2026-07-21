/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import oshi.util.Constants;

class AixNetworkParamsTest {

    @Test
    void testParseDefaultGateway() {
        // netstat -rnf inet: the "default" route's gateway is the second column
        String gw = AixNetworkParams.parseDefaultGateway(Arrays.asList(//
                "Routing tables", //
                "Destination      Gateway         Flags  Refs   Use  If  Exp  Groups", //
                "Route Tree for Protocol Family 2 (Internet):", //
                "default          192.168.1.1     UG      1     123  en0   -    -", //
                "127/8            127.0.0.1       U       3       0  lo0   -    -"));
        assertThat(gw, is("192.168.1.1"));
    }

    @Test
    void testParseDefaultGatewayNoDefaultRoute() {
        String gw = AixNetworkParams.parseDefaultGateway(Arrays.asList(//
                "Destination      Gateway         Flags  Refs   Use  If  Exp  Groups", //
                "127/8            127.0.0.1       U       3       0  lo0   -    -"));
        assertThat(gw, is(Constants.UNKNOWN));
    }

    @Test
    void testParseDefaultGatewayEmpty() {
        assertThat(AixNetworkParams.parseDefaultGateway(Collections.emptyList()), is(Constants.UNKNOWN));
    }
}
