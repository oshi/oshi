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
    private static final List<String> TCP_NETSTAT = List.of("Active Internet connections",
            "Proto Recv-Q Send-Q  Local Address          Foreign Address        (state)",
            "tcp4       0      0  192.168.1.5.55362      93.184.216.34.443      ESTABLISHED",
            "tcp4       0      0  192.168.1.5.55363      93.184.216.34.443      ESTABLISHED",
            "tcp6       0      0  ::1.55364              ::1.8080               ESTABLISHED",
            "tcp4       0      0  192.168.1.5.55365      10.0.0.1.80            TIME_WAIT");

    // Fixture: netstat -n output
    private static final List<String> NETSTAT_N = List.of("Active Internet connections",
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
    private static final List<String> UDP_STATS_LINUX = List.of("Udp:", "    12345 packets received",
            "    67 packets to unknown port received", "    0 packet receive errors", "    8901 packets sent");

    // Fixture: netstat -s -p tcp output (OpenBSD/FreeBSD style)
    private static final List<String> TCP_STATS_BSD = List.of("tcp:", "    100 packet sent", "    200 packet received",
            "    5 bad connection attempts", "    15 connection established (including accepts)",
            "    7 dropped due to RST", "    42 retransmitted 3 data packet", "    2 discarded for bad checksum",
            "    3 discarded for bad header offset field", "    10 resets sent");

    // Fixture: netstat -s -p udp output (OpenBSD/FreeBSD style)
    private static final List<String> UDP_STATS_BSD = List.of("udp:", "    5000 datagram output",
            "    3000 datagram received", "    50 dropped due to no socket",
            "    10 broadcast/multicast datagram dropped due to no socket", "    2 with incomplete header",
            "    1 with bad data length field", "    3 with bad checksum", "    4 with no checksum");

    // Fixture: netstat -n with SYN_RCVD state (should map to SYN_RECV)
    private static final List<String> NETSTAT_SYN_RCVD = List.of("Active Internet connections",
            "Proto Recv-Q Send-Q  Local Address          Foreign Address        (state)",
            "tcp4       0      0  10.0.0.1.8080          10.0.0.2.54321         SYN_RCVD");

    // Fixture: netstat -n with short lines (fewer than 5 elements) that should be ignored
    private static final List<String> NETSTAT_SHORT_LINES = List.of(
            "tcp4       0      0  192.168.1.5.55362      93.184.216.34.443      ESTABLISHED", "tcp4 short",
            "tcp4    0    0", "tcp4       0      0  192.168.1.5.55365      10.0.0.1.80            TIME_WAIT");

    // Fixture: netstat -n with IPv6 addresses
    private static final List<String> NETSTAT_IPV6 = List.of(
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
        List<String> retransmitLines = List.of("tcp:", "    99 retransmitted 5 data packet");
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

    @Test
    void testQueryNetstatIpv4MappedIpv6Address() {
        // A dual-stack socket reports an IPv4 peer in IPv6 notation
        List<IPConnection> connections = NetStat.queryNetstat(
                List.of("tcp6       0      0  ::ffff:10.0.2.15.65432 ::ffff:10.0.2.2.443    ESTABLISHED"));
        assertThat(connections, hasSize(1));
        byte[] expectedLocal = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 10, 0, 2, 15 };
        byte[] expectedForeign = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, (byte) 0xff, (byte) 0xff, 10, 0, 2, 2 };
        assertThat(connections.get(0).getLocalAddress(), is(expectedLocal));
        assertThat(connections.get(0).getLocalPort(), is(65432));
        assertThat(connections.get(0).getForeignAddress(), is(expectedForeign));
        assertThat(connections.get(0).getForeignPort(), is(443));
    }

    @Test
    void testQuerySolarisNetstat() {
        // Captured on Solaris 11.4 SPARC with Java holding IPv4 and IPv6 loopback TCP and UDP connections. The SCTP
        // section is not captured; it checks that rows after a section the parser does not read are skipped
        List<String> lines = """

                UDP: IPv4
                   Local Address        Remote Address      State      Send Buf     TxOverflows     Recv Buf     RxOverflows
                -------------------- -------------------- ---------- ------------ --------------- ------------ ---------------
                129.70.163.179.43864 129.70.161.2.53      Connected         57344               0        57344               0
                127.0.0.1.63157      127.0.0.1.9          Connected         57344               0        57344               0

                UDP: IPv6
                   Local Address                     Remote Address                   State      If    Send Buf     TxOverflows     Recv Buf     RxOverflows
                --------------------------------- --------------------------------- ---------- ----- ------------ --------------- ------------ ---------------
                ::1.63188                         ::1.9                             Connected               57344               0        57344               0

                TCP: IPv4
                   Local Address        Remote Address     Swind  Send-Q  Rwind  Recv-Q    State
                -------------------- -------------------- ------- ------ ------- ------ -----------
                129.70.163.179.22    129.70.160.90.43210    55296      0  256960      0 ESTABLISHED
                129.70.163.179.22    64.110.156.149.50538  131072     67  256296      0 ESTABLISHED
                127.0.0.1.40237      127.0.0.1.64211       269936      0  261760      0 ESTABLISHED

                TCP: IPv6
                   Local Address                     Remote Address                  Swind  Send-Q  Rwind  Recv-Q   State      If
                --------------------------------- --------------------------------- ------- ------ ------- ------ ----------- -----
                ::1.58103                         ::1.40237                          261760      0  270336      0 ESTABLISHED

                SCTP:
                        Local Address                   Remote Address          Swind  Send-Q Rwind  Recv-Q StrsI/O  State
                ------------------------------- ------------------------------- ------ ------ ------ ------ ------- -----------
                127.0.0.1.5000                  127.0.0.1.5001                   102400      0 102400      0  32/32 ESTABLISHED

                Active UNIX domain sockets
                Type       Local Address                           Remote Address
                stream-ord /var/run/dbus/system_bus_socket
                """
                .lines().toList();
        List<IPConnection> connections = NetStat.querySolarisNetstat(lines);
        assertThat(connections, hasSize(7));

        IPConnection udp4 = connections.get(1);
        assertThat(udp4.getType(), is("udp4"));
        assertThat(udp4.getLocalAddress(), is(new byte[] { 127, 0, 0, 1 }));
        assertThat(udp4.getLocalPort(), is(63157));
        assertThat(udp4.getForeignPort(), is(9));
        assertThat(udp4.getState(), is(TcpState.NONE));

        IPConnection udp6 = connections.get(2);
        assertThat(udp6.getType(), is("udp6"));
        assertThat(udp6.getLocalAddress(), is(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
        assertThat(udp6.getLocalPort(), is(63188));

        IPConnection tcp4 = connections.get(4);
        assertThat(tcp4.getType(), is("tcp4"));
        assertThat(tcp4.getForeignAddress(), is(new byte[] { 64, 110, (byte) 156, (byte) 149 }));
        assertThat(tcp4.getForeignPort(), is(50538));
        assertThat(tcp4.getTransmitQueue(), is(67));
        assertThat(tcp4.getReceiveQueue(), is(0));
        assertThat(tcp4.getState(), is(TcpState.ESTABLISHED));

        IPConnection tcp6 = connections.get(6);
        assertThat(tcp6.getType(), is("tcp6"));
        assertThat(tcp6.getLocalAddress(), is(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
        assertThat(tcp6.getForeignAddress(), is(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
        assertThat(tcp6.getLocalPort(), is(58103));
        assertThat(tcp6.getForeignPort(), is(40237));
        assertThat(tcp6.getState(), is(TcpState.ESTABLISHED));
    }

    @Test
    void testQuerySolarisNetstatStates() {
        // A row whose address starts with a letter, and the states that need mapping
        List<String> lines = """
                TCP: IPv6
                fe80::1.22                        fe80::2.50000                      49152      0  49152      0 SYN_RCVD
                fe80::1.22                        fe80::3.50001                      49152      0  49152      0 SYN_RECEIVED
                fe80::1.22                        fe80::4.50002                      49152      0  49152      0 BOUND
                """
                .lines().toList();
        List<IPConnection> connections = NetStat.querySolarisNetstat(lines);
        assertThat(connections, hasSize(3));
        assertThat(connections.get(0).getLocalAddress(),
                is(new byte[] { (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
        assertThat(connections.get(0).getState(), is(TcpState.SYN_RECV));
        // The man page spells it SYN_RECEIVED; the netstat binary prints SYN_RCVD
        assertThat(connections.get(1).getState(), is(TcpState.SYN_RECV));
        assertThat(connections.get(2).getState(), is(TcpState.UNKNOWN));
    }

    @Test
    void testQuerySolarisNetstatAll() {
        // Captured on Solaris 11.4 SPARC with netstat -an, trimmed to a few rows per section
        List<String> lines = """

                UDP: IPv4
                   Local Address        Remote Address      State      Send Buf     TxOverflows     Recv Buf     RxOverflows
                -------------------- -------------------- ---------- ------------ --------------- ------------ ---------------
                      *.*                                 Unbound           57344               0        57344               0
                127.0.0.1.123                             Idle              57344               0        57344               0
                129.70.163.179.51841 129.70.161.2.53      Connected         57344               0        57344               0

                UDP: IPv6
                   Local Address                     Remote Address                   State      If    Send Buf     TxOverflows     Recv Buf     RxOverflows
                --------------------------------- --------------------------------- ---------- ----- ------------ --------------- ------------ ---------------
                fe80::214:4fff:fefb:a5d7.123                                        Idle       net0         57344               0        57344               0

                TCP: IPv4
                   Local Address        Remote Address     Swind  Send-Q  Rwind  Recv-Q    State
                -------------------- -------------------- ------- ------ ------- ------ -----------
                      *.22                 *.*                  0      0  256000      0 LISTEN
                      *.*                  *.*                  0      0  256000      0 IDLE
                127.0.0.1.4999             *.*                  0      0  256000      0 LISTEN

                TCP: IPv6
                   Local Address                     Remote Address                  Swind  Send-Q  Rwind  Recv-Q   State      If
                --------------------------------- --------------------------------- ------- ------ ------- ------ ----------- -----
                ::1.6010                                *.*                               0      0  256000      0 LISTEN

                Active UNIX domain sockets
                """
                .lines().toList();
        List<IPConnection> connections = NetStat.querySolarisNetstat(lines);
        assertThat(connections, hasSize(8));

        IPConnection unbound = connections.get(0);
        assertThat(unbound.getLocalAddress().length, is(0));
        assertThat(unbound.getLocalPort(), is(0));
        assertThat(unbound.getForeignAddress().length, is(0));

        // The remote address column is blank, not the state
        IPConnection idle = connections.get(1);
        assertThat(idle.getLocalAddress(), is(new byte[] { 127, 0, 0, 1 }));
        assertThat(idle.getLocalPort(), is(123));
        assertThat(idle.getForeignAddress().length, is(0));
        assertThat(idle.getForeignPort(), is(0));

        IPConnection udp6 = connections.get(3);
        assertThat(udp6.getType(), is("udp6"));
        assertThat(udp6.getLocalAddress(), is(new byte[] { (byte) 0xfe, (byte) 0x80, 0, 0, 0, 0, 0, 0, 2, 0x14, 0x4f,
                (byte) 0xff, (byte) 0xfe, (byte) 0xfb, (byte) 0xa5, (byte) 0xd7 }));
        assertThat(udp6.getForeignAddress().length, is(0));

        IPConnection listen = connections.get(4);
        assertThat(listen.getType(), is("tcp4"));
        assertThat(listen.getLocalAddress().length, is(0));
        assertThat(listen.getLocalPort(), is(22));
        assertThat(listen.getForeignAddress().length, is(0));
        assertThat(listen.getState(), is(TcpState.LISTEN));
        assertThat(connections.get(5).getState(), is(TcpState.UNKNOWN));
        assertThat(connections.get(6).getLocalAddress(), is(new byte[] { 127, 0, 0, 1 }));

        IPConnection listen6 = connections.get(7);
        assertThat(listen6.getType(), is("tcp6"));
        assertThat(listen6.getLocalAddress(), is(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 }));
        assertThat(listen6.getLocalPort(), is(6010));
        assertThat(listen6.getState(), is(TcpState.LISTEN));
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
