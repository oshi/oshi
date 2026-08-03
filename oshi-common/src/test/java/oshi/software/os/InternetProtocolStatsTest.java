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

    @Test
    void testFromBsdState() {
        // BSD tcp_fsm.h TCPS_* ordering, as reported by the macOS tcpsi_state field
        assertThat(TcpState.fromBsdState(0), is(TcpState.CLOSED));
        assertThat(TcpState.fromBsdState(1), is(TcpState.LISTEN));
        assertThat(TcpState.fromBsdState(2), is(TcpState.SYN_SENT));
        assertThat(TcpState.fromBsdState(3), is(TcpState.SYN_RECV));
        assertThat(TcpState.fromBsdState(4), is(TcpState.ESTABLISHED));
        assertThat(TcpState.fromBsdState(5), is(TcpState.CLOSE_WAIT));
        assertThat(TcpState.fromBsdState(6), is(TcpState.FIN_WAIT_1));
        assertThat(TcpState.fromBsdState(7), is(TcpState.CLOSING));
        assertThat(TcpState.fromBsdState(8), is(TcpState.LAST_ACK));
        assertThat(TcpState.fromBsdState(9), is(TcpState.FIN_WAIT_2));
        assertThat(TcpState.fromBsdState(10), is(TcpState.TIME_WAIT));
        // Out-of-range codes fall through to UNKNOWN
        assertThat(TcpState.fromBsdState(-1), is(TcpState.UNKNOWN));
        assertThat(TcpState.fromBsdState(11), is(TcpState.UNKNOWN));
    }

    @Test
    void testFromWindowsMibState() {
        // Windows MIB_TCP_STATE is 1-based; note the FIN_WAIT and CLOSE_WAIT ordering differs from BSD
        assertThat(TcpState.fromWindowsMibState(1), is(TcpState.CLOSED));
        assertThat(TcpState.fromWindowsMibState(2), is(TcpState.LISTEN));
        assertThat(TcpState.fromWindowsMibState(3), is(TcpState.SYN_SENT));
        assertThat(TcpState.fromWindowsMibState(4), is(TcpState.SYN_RECV));
        assertThat(TcpState.fromWindowsMibState(5), is(TcpState.ESTABLISHED));
        assertThat(TcpState.fromWindowsMibState(6), is(TcpState.FIN_WAIT_1));
        assertThat(TcpState.fromWindowsMibState(7), is(TcpState.FIN_WAIT_2));
        assertThat(TcpState.fromWindowsMibState(8), is(TcpState.CLOSE_WAIT));
        assertThat(TcpState.fromWindowsMibState(9), is(TcpState.CLOSING));
        assertThat(TcpState.fromWindowsMibState(10), is(TcpState.LAST_ACK));
        assertThat(TcpState.fromWindowsMibState(11), is(TcpState.TIME_WAIT));
        // The DELETE_TCB pseudo-state (12) maps to CLOSED
        assertThat(TcpState.fromWindowsMibState(12), is(TcpState.CLOSED));
        // 0 is not a valid MIB state, and codes past the enum fall through to UNKNOWN
        assertThat(TcpState.fromWindowsMibState(0), is(TcpState.UNKNOWN));
        assertThat(TcpState.fromWindowsMibState(13), is(TcpState.UNKNOWN));
    }

    @Test
    void testFromLinuxState() {
        // Linux kernel TCP_* ordering, as reported in /proc/net/tcp (hex)
        assertThat(TcpState.fromLinuxState(0x01), is(TcpState.ESTABLISHED));
        assertThat(TcpState.fromLinuxState(0x02), is(TcpState.SYN_SENT));
        assertThat(TcpState.fromLinuxState(0x03), is(TcpState.SYN_RECV));
        assertThat(TcpState.fromLinuxState(0x04), is(TcpState.FIN_WAIT_1));
        assertThat(TcpState.fromLinuxState(0x05), is(TcpState.FIN_WAIT_2));
        assertThat(TcpState.fromLinuxState(0x06), is(TcpState.TIME_WAIT));
        assertThat(TcpState.fromLinuxState(0x07), is(TcpState.CLOSED));
        assertThat(TcpState.fromLinuxState(0x08), is(TcpState.CLOSE_WAIT));
        assertThat(TcpState.fromLinuxState(0x09), is(TcpState.LAST_ACK));
        assertThat(TcpState.fromLinuxState(0x0A), is(TcpState.LISTEN));
        assertThat(TcpState.fromLinuxState(0x0B), is(TcpState.CLOSING));
        // 0x00 is not a valid state, and codes past the enum fall through to UNKNOWN
        assertThat(TcpState.fromLinuxState(0x00), is(TcpState.UNKNOWN));
        assertThat(TcpState.fromLinuxState(0x0C), is(TcpState.UNKNOWN));
    }
}
