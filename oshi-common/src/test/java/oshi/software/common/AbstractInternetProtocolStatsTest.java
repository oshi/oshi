/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

class AbstractInternetProtocolStatsTest {

    private static final AbstractInternetProtocolStats STATS = new AbstractInternetProtocolStats() {
        @Override
        public TcpStats getTCPv4Stats() {
            return new TcpStats(1L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        }

        @Override
        public UdpStats getUDPv4Stats() {
            return new UdpStats(1L, 0L, 0L, 0L);
        }
    };

    @Test
    void testDefaultTCPv6StatsAllZeros() {
        TcpStats tcp6 = STATS.getTCPv6Stats();
        assertThat(tcp6.getConnectionsEstablished(), is(0L));
        assertThat(tcp6.getConnectionsActive(), is(0L));
        assertThat(tcp6.getConnectionsPassive(), is(0L));
        assertThat(tcp6.getConnectionFailures(), is(0L));
        assertThat(tcp6.getConnectionsReset(), is(0L));
        assertThat(tcp6.getSegmentsSent(), is(0L));
        assertThat(tcp6.getSegmentsReceived(), is(0L));
        assertThat(tcp6.getSegmentsRetransmitted(), is(0L));
        assertThat(tcp6.getInErrors(), is(0L));
        assertThat(tcp6.getOutResets(), is(0L));
    }

    @Test
    void testDefaultUDPv6StatsAllZeros() {
        UdpStats udp6 = STATS.getUDPv6Stats();
        assertThat(udp6.getDatagramsSent(), is(0L));
        assertThat(udp6.getDatagramsReceived(), is(0L));
        assertThat(udp6.getDatagramsNoPort(), is(0L));
        assertThat(udp6.getDatagramsReceivedErrors(), is(0L));
    }
}
