/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpState;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

class InternetProtocolStatsTest {

    @Test
    void testTcpStatsGetters() {
        TcpStats stats = new TcpStats(10L, 5L, 3L, 1L, 2L, 100L, 90L, 4L, 0L, 1L);
        assertThat(stats.getConnectionsEstablished(), is(10L));
        assertThat(stats.getConnectionsActive(), is(5L));
        assertThat(stats.getConnectionsPassive(), is(3L));
        assertThat(stats.getConnectionFailures(), is(1L));
        assertThat(stats.getConnectionsReset(), is(2L));
        assertThat(stats.getSegmentsSent(), is(100L));
        assertThat(stats.getSegmentsReceived(), is(90L));
        assertThat(stats.getSegmentsRetransmitted(), is(4L));
        assertThat(stats.getInErrors(), is(0L));
        assertThat(stats.getOutResets(), is(1L));
        assertThat(stats.toString(), containsString("10"));
    }

    @Test
    void testUdpStatsGetters() {
        UdpStats stats = new UdpStats(50L, 45L, 2L, 1L);
        assertThat(stats.getDatagramsSent(), is(50L));
        assertThat(stats.getDatagramsReceived(), is(45L));
        assertThat(stats.getDatagramsNoPort(), is(2L));
        assertThat(stats.getDatagramsReceivedErrors(), is(1L));
        assertThat(stats.toString(), containsString("50"));
    }

    @Test
    void testIPConnectionGetters() {
        byte[] local = { 127, 0, 0, 1 };
        byte[] foreign = { 10, 0, 0, 1 };
        IPConnection conn = new IPConnection("tcp4", local, 8080, foreign, 443, TcpState.ESTABLISHED, 0, 0, 1234);
        assertThat(conn.getType(), is("tcp4"));
        assertThat(conn.getLocalPort(), is(8080));
        assertThat(conn.getForeignPort(), is(443));
        assertThat(conn.getState(), is(TcpState.ESTABLISHED));
        assertThat(conn.getTransmitQueue(), is(0));
        assertThat(conn.getReceiveQueue(), is(0));
        assertThat(conn.getowningProcessId(), is(1234));
        assertThat(conn.getLocalAddress().length, is(4));
        assertArrayEquals(local, conn.getLocalAddress());
        assertThat(conn.getForeignAddress().length, is(4));
        assertArrayEquals(foreign, conn.getForeignAddress());
        assertThat(conn.toString(), containsString("tcp4"));
    }

    @Test
    void testTcpStateEnum() {
        assertThat(TcpState.valueOf("ESTABLISHED"), is(TcpState.ESTABLISHED));
        assertThat(TcpState.valueOf("CLOSED"), is(TcpState.CLOSED));
        assertThat(TcpState.valueOf("UNKNOWN"), is(TcpState.UNKNOWN));
        assertThrows(IllegalArgumentException.class, () -> TcpState.valueOf("INVALID"));
    }
}
