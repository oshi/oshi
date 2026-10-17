/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.mac;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.InternetProtocolStats.TcpStats;

class MacInternetProtocolStatsTest {

    /**
     * Counters read on macOS 26 by an unprivileged user, who sees zeros from {@code net.inet.tcp.stats} and so gets the
     * estimate from the IP and UDP totals. netstat printed them as 258570565 "packets sent from this host", 220788136
     * "total packets received" and 220792384 "packets for this host" (delivered), with 6116717 UDP datagrams output and
     * 23153420 received.
     */
    private static final class Unprivileged extends MacInternetProtocolStats {
        Unprivileged() {
            super(false);
        }

        @Override
        protected BsdTcpstat queryTcpstat() {
            return new BsdTcpstat(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        @Override
        protected BsdUdpstat queryUdpstat() {
            return new BsdUdpstat(23_153_420, 0, 0, 0, 0, 0, 6_116_717, 0, 0);
        }

        @Override
        protected BsdIpstat queryIpstat() {
            return new BsdIpstat(220_788_136, 0, 0, 0, 0, 0, 258_570_565);
        }

        @Override
        protected BsdIp6stat queryIp6stat() {
            return new BsdIp6stat(0L, 0L);
        }
    }

    @Test
    void testTcpv4EstimateCountsOutboundAsSent() {
        TcpStats tcp = new Unprivileged().getTCPv4Stats();
        // IP packets generated here, less the UDP datagrams among them. Not the packets delivered to this host, which
        // are inbound
        assertThat(tcp.getSegmentsSent(), is(258_570_565L - 6_116_717L));
        assertThat(tcp.getSegmentsReceived(), is(220_788_136L - 23_153_420L));
    }
}
