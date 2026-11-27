/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.hardware.NetworkIF.IfOperStatus;

class IfconfigTest {

    // AIX 7.3 (cfarm gcc119): no status line, so RUNNING decides
    private static final List<String> AIX_EN0 = """
            en0: flags=1e084863,81cc0<UP,BROADCAST,NOTRAILERS,RUNNING,SIMPLEX,MULTICAST,GROUPRT,64BIT,CHECKSUM_OFFLOAD(ACTIVE),LARGESEND,CHAIN>
            \tinet 10.1.0.3 netmask 0xfffffe00 broadcast 10.1.1.255
            \t tcp_sendspace 262144 tcp_recvspace 262144 rfc1323 1
            """
            .lines().toList();

    private static final List<String> AIX_LO0 = """
            lo0: flags=e08084b,c0<UP,BROADCAST,LOOPBACK,RUNNING,SIMPLEX,MULTICAST,GROUPRT,64BIT,LARGESEND,CHAIN>
            \tinet 127.0.0.1 netmask 0xff000000 broadcast 127.255.255.255
            \tinet6 ::1%1/64
            \t tcp_sendspace 131072 tcp_recvspace 131072 rfc1323 1
            """.lines().toList();

    @Test
    void testParseOperStatusAix() {
        assertThat(Ifconfig.parseOperStatus(AIX_EN0), is(IfOperStatus.UP));
        assertThat(Ifconfig.parseOperStatus(AIX_LO0), is(IfOperStatus.UP));
        // Administratively up, no link
        assertThat(
                Ifconfig.parseOperStatus(List
                        .of("en2: flags=1e084843,480<UP,BROADCAST,NOTRAILERS,SIMPLEX,MULTICAST,GROUPRT,64BIT,CHAIN>")),
                is(IfOperStatus.DOWN));
        // ifconfig en2 down
        assertThat(
                Ifconfig.parseOperStatus(
                        List.of("en2: flags=1e084842,480<BROADCAST,NOTRAILERS,SIMPLEX,MULTICAST,GROUPRT,64BIT,CHAIN>")),
                is(IfOperStatus.DOWN));
        // ifconfig on an interface that does not exist writes its error to stderr, leaving no output
        assertThat(Ifconfig.parseOperStatus(List.of()), is(IfOperStatus.UNKNOWN));
    }
}
