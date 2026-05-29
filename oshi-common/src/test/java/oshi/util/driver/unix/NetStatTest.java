/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util.driver.unix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpState;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;
import oshi.util.tuples.Pair;

class NetStatTest {

    // Fixture: netstat -n -p tcp output (macOS style)
    private static final List<String> TCP_NETSTAT = Arrays.asList("Active Internet connections",
            "Proto Recv-Q Send-Q  Local Address          Foreign Address        (state)",
            "tcp4       0      0  192.168.1.5.55362      93.184.216.34.443      ESTABLISHED",
            "tcp4       0      0  192.168.1.5.55363      93.184.216.34.443      ESTABLISHED",
            "tcp6       0      0  ::1.55364              ::1.8080               ESTABLISHED",
            "tcp4       0      0  192.168.1.5.55365      10.0.0.1.80            TIME_WAIT");

    // Fixture: netstat -n output
    private static final List<String> NETSTAT_N = Arrays.asList("Active Internet connections",
            "Proto Recv-Q Send-Q  Local Address          Foreign Address        (state)",
            "tcp4       0      0  192.168.1.5.55362      93.184.216.34.443      ESTABLISHED",
            "udp4       0      0  192.168.1.5.5353       *.* ");

    // Fixture: netstat -st4 output (Linux TCP stats)
    private static final List<String> TCP_STATS_LINUX = Arrays.asList("Tcp:", "    1234 active connection openings",
            "    5678 passive connection openings", "    12 failed connection attempts",
            "    34 connection resets received", "    56 connections established", "    789012 segments received",
            "    345678 segments sent out", "    901 segments retransmitted", "    0 bad segments received",
            "    45 resets sent");

    // Fixture: netstat -su4 output (Linux UDP stats)
    private static final List<String> UDP_STATS_LINUX = Arrays.asList("Udp:", "    12345 packets received",
            "    67 packets to unknown port received", "    0 packet receive errors", "    8901 packets sent");

    // Fixture: netstat -s -p tcp output (OpenBSD/FreeBSD style)
    private static final List<String> TCP_STATS_BSD = Arrays.asList("tcp:", "    100 packet sent",
            "    200 packet received", "    5 bad connection attempts",
            "    15 connection established (including accepts)", "    7 dropped due to RST",
            "    42 retransmitted 3 data packet", "    2 discarded for bad checksum",
            "    3 discarded for bad header offset field", "    10 resets sent");

    // Fixture: netstat -s -p udp output (OpenBSD/FreeBSD style)
    private static final List<String> UDP_STATS_BSD = Arrays.asList("udp:", "    5000 datagram output",
            "    3000 datagram received", "    50 dropped due to no socket",
            "    10 broadcast/multicast datagram dropped due to no socket", "    2 with incomplete header",
            "    1 with bad data length field", "    3 with bad checksum", "    4 with no checksum");

    // Fixture: netstat -n with SYN_RCVD state (should map to SYN_RECV)
    private static final List<String> NETSTAT_SYN_RCVD = Arrays.asList("Active Internet connections",
            "Proto Recv-Q Send-Q  Local Address          Foreign Address        (state)",
            "tcp4       0      0  10.0.0.1.8080          10.0.0.2.54321         SYN_RCVD");

    // Fixture: netstat -n with short lines (fewer than 5 elements) that should be ignored
    private static final List<String> NETSTAT_SHORT_LINES = Arrays.asList(
            "tcp4       0      0  192.168.1.5.55362      93.184.216.34.443      ESTABLISHED", "tcp4 short",
            "tcp4    0    0", "tcp4       0      0  192.168.1.5.55365      10.0.0.1.80            TIME_WAIT");

    // Fixture: netstat -n with IPv6 addresses
    private static final List<String> NETSTAT_IPV6 = Arrays.asList(
            "tcp6       0      0  2001:db8::1.443        2001:db8::2.54321      ESTABLISHED",
            "tcp6       0      0  fe80::1:.8080          fe80::2:.9090          TIME_WAIT",
            "tcp6       0      0  ::1.55364              ::1.8080               ESTABLISHED");

    @Test
    void testQueryTcpnetstat() {
        Pair<Long, Long> result = NetStat.queryTcpnetstat(TCP_NETSTAT);
        assertThat(result.getA(), is(2L));
        assertThat(result.getB(), is(1L));
    }

    @Test
    void testQueryTcpnetstatEmpty() {
        Pair<Long, Long> result = NetStat.queryTcpnetstat(Collections.emptyList());
        assertThat(result.getA(), is(0L));
        assertThat(result.getB(), is(0L));
    }

    @Test
    void testQueryNetstat() {
        List<IPConnection> connections = NetStat.queryNetstat(NETSTAT_N);
        assertThat(connections, hasSize(2));
        assertThat(connections.get(0).getType(), is("tcp4"));
        assertThat(connections.get(0).getState(), is(TcpState.ESTABLISHED));
        assertThat(connections.get(0).getLocalPort(), is(55362));
        assertThat(connections.get(0).getForeignPort(), is(443));
        assertThat(connections.get(1).getType(), is("udp4"));
        assertThat(connections.get(1).getState(), is(TcpState.NONE));
        assertThat(connections.get(1).getLocalPort(), is(5353));
    }

    @Test
    void testQueryNetstatEmpty() {
        assertThat(NetStat.queryNetstat(Collections.emptyList()), is(empty()));
    }

    @Test
    void testQueryTcpStatsLinux() {
        TcpStats stats = NetStat.queryTcpStats(TCP_STATS_LINUX);
        assertThat(stats.getConnectionsEstablished(), is(56L));
        assertThat(stats.getConnectionsActive(), is(1234L));
        assertThat(stats.getConnectionsPassive(), is(5678L));
        assertThat(stats.getConnectionFailures(), is(12L));
        assertThat(stats.getConnectionsReset(), is(34L));
        assertThat(stats.getSegmentsSent(), is(345678L));
        assertThat(stats.getSegmentsReceived(), is(789012L));
        assertThat(stats.getSegmentsRetransmitted(), is(901L));
        assertThat(stats.getOutResets(), is(45L));
        assertThat(stats.getInErrors(), is(0L));
    }

    @Test
    void testQueryTcpStatsEmpty() {
        TcpStats stats = NetStat.queryTcpStats(Collections.emptyList());
        assertThat(stats.getConnectionsEstablished(), is(0L));
        assertThat(stats.getConnectionsActive(), is(0L));
        assertThat(stats.getSegmentsReceived(), is(0L));
        assertThat(stats.getInErrors(), is(0L));
    }

    @Test
    void testQueryUdpStatsLinux() {
        UdpStats stats = NetStat.queryUdpStats(UDP_STATS_LINUX);
        assertThat(stats.getDatagramsSent(), is(8901L));
        assertThat(stats.getDatagramsReceived(), is(12345L));
        assertThat(stats.getDatagramsNoPort(), is(67L));
        assertThat(stats.getDatagramsReceivedErrors(), is(0L));
    }

    @Test
    void testQueryUdpStatsEmpty() {
        UdpStats stats = NetStat.queryUdpStats(Collections.emptyList());
        assertThat(stats.getDatagramsSent(), is(0L));
        assertThat(stats.getDatagramsReceived(), is(0L));
        assertThat(stats.getDatagramsNoPort(), is(0L));
        assertThat(stats.getDatagramsReceivedErrors(), is(0L));
    }

    @Test
    void testQueryTcpStatsBsd() {
        TcpStats stats = NetStat.queryTcpStats(TCP_STATS_BSD);
        assertThat(stats.getConnectionsEstablished(), is(15L));
        assertThat(stats.getConnectionFailures(), is(5L));
        assertThat(stats.getConnectionsReset(), is(7L));
        assertThat(stats.getSegmentsSent(), is(100L));
        assertThat(stats.getSegmentsReceived(), is(200L));
        // "42 retransmitted 3 data packet" matches the special-case pattern
        assertThat(stats.getSegmentsRetransmitted(), is(42L));
        // 2 bad checksum + 3 bad header offset field
        assertThat(stats.getInErrors(), is(5L));
        assertThat(stats.getOutResets(), is(10L));
    }

    @Test
    void testQueryTcpStatsRetransmitSpecialCase() {
        // Test the "N retransmitted M data packet" pattern in isolation (BSD format)
        List<String> retransmitLines = Arrays.asList("tcp:", "    99 retransmitted 5 data packet");
        TcpStats stats = NetStat.queryTcpStats(retransmitLines);
        // The special-case pattern uses += on segmentsRetransmitted
        assertThat(stats.getSegmentsRetransmitted(), is(99L));
    }

    @Test
    void testQueryUdpStatsBsd() {
        UdpStats stats = NetStat.queryUdpStats(UDP_STATS_BSD);
        assertThat(stats.getDatagramsSent(), is(5000L));
        assertThat(stats.getDatagramsReceived(), is(3000L));
        // 50 "dropped due to no socket" + 10 "broadcast/multicast datagram dropped due to no socket"
        assertThat(stats.getDatagramsNoPort(), is(60L));
        // 2 "with incomplete header" + 1 "with bad data length field" + 3 "with bad checksum" + 4 "with no checksum"
        assertThat(stats.getDatagramsReceivedErrors(), is(10L));
    }

    @Test
    void testQueryNetstatSynRcvdSubstitution() {
        List<IPConnection> connections = NetStat.queryNetstat(NETSTAT_SYN_RCVD);
        assertThat(connections, hasSize(1));
        assertThat(connections.get(0).getState(), is(TcpState.SYN_RECV));
        assertThat(connections.get(0).getLocalPort(), is(8080));
        assertThat(connections.get(0).getForeignPort(), is(54321));
    }

    @Test
    void testQueryNetstatShortLinesIgnored() {
        List<IPConnection> connections = NetStat.queryNetstat(NETSTAT_SHORT_LINES);
        // Only lines with 5+ split elements should produce connections
        assertThat(connections, hasSize(2));
        assertThat(connections.get(0).getState(), is(TcpState.ESTABLISHED));
        assertThat(connections.get(1).getState(), is(TcpState.TIME_WAIT));
    }

    @Test
    void testQueryNetstatIpv6Addresses() {
        List<IPConnection> connections = NetStat.queryNetstat(NETSTAT_IPV6);
        assertThat(connections, hasSize(3));
        // First: 2001:db8::1 port 443
        assertThat(connections.get(0).getType(), is("tcp6"));
        assertThat(connections.get(0).getLocalPort(), is(443));
        assertThat(connections.get(0).getForeignPort(), is(54321));
        assertThat(connections.get(0).getState(), is(TcpState.ESTABLISHED));
        // Second: fe80::1: port 8080 (trailing colon in IP, tests parseIP fallback)
        assertThat(connections.get(1).getType(), is("tcp6"));
        assertThat(connections.get(1).getLocalPort(), is(8080));
        assertThat(connections.get(1).getForeignPort(), is(9090));
        assertThat(connections.get(1).getState(), is(TcpState.TIME_WAIT));
        // Third: ::1 port 55364
        assertThat(connections.get(2).getType(), is("tcp6"));
        assertThat(connections.get(2).getLocalPort(), is(55364));
        assertThat(connections.get(2).getForeignPort(), is(8080));
        assertThat(connections.get(2).getState(), is(TcpState.ESTABLISHED));
    }

    @Nested
    @DisabledOnOs(OS.WINDOWS)
    class LiveTests {
        // netstat -n output format varies across platforms; live parsing only verified on FreeBSD/OpenBSD
        @DisabledOnOs({ OS.WINDOWS, OS.MAC, OS.LINUX })
        @Test
        void testQueryNetstat() {
            for (IPConnection conn : NetStat.queryNetstat()) {
                assertThat("Connection is tcp or udp", conn.getType(), anyOf(startsWith("tcp"), startsWith("udp")));
            }
        }

        @EnabledOnOs({ OS.MAC, OS.FREEBSD })
        @Test
        void testQueryTcpNetstat() {
            Pair<Long, Long> tcpConns = NetStat.queryTcpnetstat();
            assertThat("ipv4 connections must be nonnegative", tcpConns.getA().intValue(), greaterThanOrEqualTo(0));
            assertThat("ipv6 connections must be nonnegative", tcpConns.getB().intValue(), greaterThanOrEqualTo(0));
        }

        @EnabledOnOs(OS.LINUX)
        @Test
        void testQueryStatsLinux() {
            TcpStats tcpStats = NetStat.queryTcpStats("netstat -st4");
            assertThat("tcp connections must be nonnegative", tcpStats.getConnectionsEstablished(),
                    greaterThanOrEqualTo(0L));
            UdpStats udp4Stats = NetStat.queryUdpStats("netstat -su4");
            assertThat("udp4 datagrams sent must be nonnegative", udp4Stats.getDatagramsSent(),
                    greaterThanOrEqualTo(0L));
        }

        @EnabledOnOs(OS.OPENBSD)
        @Test
        void testQueryStatsOpenBSD() {
            TcpStats tcpStats = NetStat.queryTcpStats("netstat -s -p tcp");
            assertThat("tcp connections must be nonnegative", tcpStats.getConnectionsEstablished(),
                    greaterThanOrEqualTo(0L));
            UdpStats udpStats = NetStat.queryUdpStats("netstat -s -p udp");
            assertThat("udp datagrams sent must be nonnegative", udpStats.getDatagramsSent(), greaterThanOrEqualTo(0L));
        }
    }
}
