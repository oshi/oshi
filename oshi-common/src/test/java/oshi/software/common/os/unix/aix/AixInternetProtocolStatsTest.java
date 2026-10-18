/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.aix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

class AixInternetProtocolStatsTest {

    @Test
    void testTcpStats() {
        // perfstat_protocol's tcp counters on AIX 7.3 (cfarm119), read beside netstat -s: 174587 dropped is netstat's
        // "connections closed (including 174587 drops)", while its 74322 embryonic drops have no perfstat counter
        TcpStats tcp = AixInternetProtocolStats.tcpStats(2_030_200L, 276_308L, 1_972_702L, 174_587L, 30_915_756L,
                57_617_958L, 303_957L);
        assertThat(tcp.getConnectionsEstablished(), is(2_030_200L));
        assertThat(tcp.getConnectionsActive(), is(276_308L));
        assertThat(tcp.getConnectionsPassive(), is(1_972_702L));
        assertThat(tcp.getConnectionsReset(), is(174_587L));
        // Not the drops again: perfstat cannot report failures or retransmissions
        assertThat(tcp.getConnectionFailures(), is(0L));
        assertThat(tcp.getSegmentsRetransmitted(), is(0L));
        assertThat(tcp.getSegmentsSent(), is(30_915_756L));
        assertThat(tcp.getSegmentsReceived(), is(57_617_958L));
        assertThat(tcp.getInErrors(), is(303_957L));
    }

    @Test
    void testUdpStats() {
        // The same machine's udp counters. perfstat's ierrors of 12442997 was netstat's 868999 dropped due to no
        // socket, 11573609 broadcast or multicast dropped due to no socket and 374 bad checksums, near enough
        UdpStats udp = AixInternetProtocolStats.udpStats(7_035_856L, 18_748_068L, 868_999L);
        assertThat(udp.getDatagramsSent(), is(7_035_856L));
        assertThat(udp.getDatagramsReceived(), is(18_748_068L));
        assertThat(udp.getDatagramsNoPort(), is(868_999L));
        assertThat(udp.getDatagramsReceivedErrors(), is(0L));
    }
}
