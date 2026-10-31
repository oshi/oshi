/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpState;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;
import oshi.util.ParseUtil;
import oshi.util.ProcUtil;

/**
 * Tests the mapping of {@code /proc/net/snmp} and {@code /proc/net/snmp6} counters onto the reported statistics. Every
 * counter in the fixtures below holds a distinct value, so reading one in the place of another fails.
 */
class LinuxInternetProtocolStatsTest {

    /**
     * An excerpt of {@code /proc/net/snmp}. Note that the kernel emits the UDP datagrams sent fourth and the datagrams
     * received first, so reading the columns by position rather than by name reports each field one place off.
     */
    private static final String SNMP = """
            Ip: Forwarding DefaultTTL InReceives InHdrErrors
            Ip: 2 64 66402 0
            Tcp: RtoAlgorithm RtoMin RtoMax MaxConn ActiveOpens PassiveOpens AttemptFails EstabResets CurrEstab InSegs OutSegs RetransSegs InErrs OutRsts InCsumErrors
            Tcp: 1 200 120000 -1 1892 73 1138 14 2 63131 61550 510 156 333 0
            Udp: InDatagrams NoPorts InErrors OutDatagrams RcvbufErrors SndbufErrors InCsumErrors IgnoredMulti MemErrors
            Udp: 7805 222 11 4202 1 2 3 2866 4
            UdpLite: InDatagrams NoPorts InErrors OutDatagrams RcvbufErrors SndbufErrors InCsumErrors IgnoredMulti MemErrors
            UdpLite: 91 92 93 94 0 0 0 0 0
            """;

    /** An excerpt of {@code /proc/net/snmp6}, including the counters that must not be read as UDPv6 ones. */
    private static final String SNMP6 = """
            Ip6InReceives                   8026
            Ip6InHdrErrors                  0
            Icmp6InMsgs                     2
            Udp6InDatagrams                 8021
            Udp6NoPorts                     22
            Udp6InErrors                    33
            Udp6OutDatagrams                119
            Udp6RcvbufErrors                1
            Udp6IgnoredMulti                44
            UdpLite6InDatagrams             91
            UdpLite6NoPorts                 92
            UdpLite6InErrors                93
            UdpLite6OutDatagrams            94
            """;

    // Verbatim /proc/net/tcp and tcp6 from a little-endian (aarch64) container, with 2001:db8:1122:3344::1 added to lo
    // so the byte order is unambiguous. The matching `ss -tna` reported:
    // CLOSE-WAIT 127.0.0.1:46143 127.0.0.1:4949
    // LISTEN [2001:db8:1122:3344::1]:22 [::]:*
    private static final List<String> TCP_LITTLE_ENDIAN = """
              sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
               0: 0100007F:B43F 0100007F:1355 08 00000000:00000000 00:00000000 00000000     0        0 11040717 2 0000000000000000 20 4 0 10 -1
            """
            .lines().toList();
    private static final List<String> TCP6_LITTLE_ENDIAN = """
              sl  local_address                         remote_address                        st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
               0: B80D0120443322110000000001000000:0016 00000000000000000000000000000000:0000 0A 00000000:00000000 00:00000000 00000000     0        0 11046942 1 0000000000000000 100 0 0 10 0
            """
            .lines().toList();

    // Verbatim /proc/net/tcp and tcp6 from a big-endian s390x host, whose kernel prints each address in network order.
    // The matching `ss -tna` reported:
    // ESTAB 148.100.75.242:22 73.83.45.212:51011
    // LISTEN [2620:91:0:657:f816:3eff:fe5f:2052]:50123 [::]:*
    private static final List<String> TCP_BIG_ENDIAN = """
              sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
               5: 94644BF2:0016 49532DD4:C743 01 00000048:00000000 01:0000001C 00000000     0        0 28625262 4 00000000164d8b11 28 4 15 10 -1
            """
            .lines().toList();
    private static final List<String> TCP6_BIG_ENDIAN = """
              sl  local_address                         remote_address                        st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode
               2: 2620009100000657F8163EFFFE5F2052:C3CB 00000000000000000000000000000000:0000 0A 00000000:00000000 00:00000000 00000000 61873        0 28684156 1 00000000da99671d 100 0 0 10 0
            """
            .lines().toList();

    private static String write(Path dir, String name, String contents) throws IOException {
        return Files.writeString(dir.resolve(name), contents).toString();
    }

    private static Map<String, Long> snmpStats(Path dir, String key) throws IOException {
        Map<String, Long> stats = ProcUtil.parseNestedStatistics(write(dir, "snmp", SNMP), key).get(key);
        assertNotNull(stats, () -> "no " + key + " statistics in the fixture");
        return stats;
    }

    @Test
    void testTcp4Stats(@TempDir Path dir) throws IOException {
        TcpStats tcp = LinuxInternetProtocolStats.parseTcp4Stats(snmpStats(dir, "Tcp"));

        assertThat("connections established", tcp.getConnectionsEstablished(), is(2L));
        assertThat("active opens", tcp.getConnectionsActive(), is(1892L));
        assertThat("passive opens", tcp.getConnectionsPassive(), is(73L));
        assertThat("attempt fails", tcp.getConnectionFailures(), is(1138L));
        assertThat("established resets", tcp.getConnectionsReset(), is(14L));
        assertThat("segments sent", tcp.getSegmentsSent(), is(61550L));
        assertThat("segments received", tcp.getSegmentsReceived(), is(63131L));
        assertThat("segments retransmitted", tcp.getSegmentsRetransmitted(), is(510L));
        assertThat("in errors", tcp.getInErrors(), is(156L));
        assertThat("out resets", tcp.getOutResets(), is(333L));
    }

    @Test
    void testUdp4Stats(@TempDir Path dir) throws IOException {
        UdpStats udp = LinuxInternetProtocolStats.parseUdp4Stats(snmpStats(dir, "Udp"));

        assertThat("datagrams sent", udp.getDatagramsSent(), is(4202L));
        assertThat("datagrams received", udp.getDatagramsReceived(), is(7805L));
        assertThat("datagrams with no port", udp.getDatagramsNoPort(), is(222L));
        assertThat("datagrams received with errors", udp.getDatagramsReceivedErrors(), is(11L));
    }

    @Test
    void testUdp6Stats(@TempDir Path dir) throws IOException {
        UdpStats udp = LinuxInternetProtocolStats.parseUdp6Stats(ProcUtil.parseStatistics(write(dir, "snmp6", SNMP6)));

        assertThat("datagrams sent", udp.getDatagramsSent(), is(119L));
        assertThat("datagrams received", udp.getDatagramsReceived(), is(8021L));
        assertThat("datagrams with no port", udp.getDatagramsNoPort(), is(22L));
        assertThat("datagrams received with errors", udp.getDatagramsReceivedErrors(), is(33L));
    }

    /** A kernel built without a protocol emits no counters for it, which must report as zeros rather than throw. */
    @Test
    void testStatsAbsent() {
        TcpStats tcp = LinuxInternetProtocolStats.parseTcp4Stats(Map.of());
        assertThat("connections established", tcp.getConnectionsEstablished(), is(0L));
        assertThat("segments sent", tcp.getSegmentsSent(), is(0L));

        for (UdpStats udp : new UdpStats[] { LinuxInternetProtocolStats.parseUdp4Stats(Map.of()),
                LinuxInternetProtocolStats.parseUdp6Stats(Map.of()) }) {
            assertThat("datagrams sent", udp.getDatagramsSent(), is(0L));
            assertThat("datagrams received", udp.getDatagramsReceived(), is(0L));
            assertThat("datagrams with no port", udp.getDatagramsNoPort(), is(0L));
            assertThat("datagrams received with errors", udp.getDatagramsReceivedErrors(), is(0L));
        }
    }

    @Test
    void testConnectionsLittleEndian() {
        List<IPConnection> tcp = LinuxInternetProtocolStats.parseConnections("tcp4", TCP_LITTLE_ENDIAN,
                Map.of(11040717L, 42), ByteOrder.LITTLE_ENDIAN);
        assertThat("The header line should be skipped", tcp.size(), is(1));
        IPConnection conn = tcp.get(0);
        assertThat(conn.getType(), is("tcp4"));
        assertThat(conn.getLocalAddress(), is(ParseUtil.parseIpv4AddressToBytes("127.0.0.1")));
        assertThat(conn.getLocalPort(), is(46143));
        assertThat(conn.getForeignAddress(), is(ParseUtil.parseIpv4AddressToBytes("127.0.0.1")));
        assertThat(conn.getForeignPort(), is(4949));
        assertThat(conn.getState(), is(TcpState.CLOSE_WAIT));
        assertThat(conn.getowningProcessId(), is(42));

        IPConnection conn6 = LinuxInternetProtocolStats
                .parseConnections("tcp6", TCP6_LITTLE_ENDIAN, Map.of(), ByteOrder.LITTLE_ENDIAN).get(0);
        assertThat(conn6.getLocalAddress(), is(ParseUtil.parseIpv6AddressToBytes("2001:db8:1122:3344::1")));
        assertThat(conn6.getLocalPort(), is(22));
        assertThat(conn6.getForeignAddress(), is(new byte[16]));
        assertThat(conn6.getState(), is(TcpState.LISTEN));
        assertThat(conn6.getowningProcessId(), is(-1));
    }

    @Test
    void testConnectionsBigEndian() {
        IPConnection conn = LinuxInternetProtocolStats
                .parseConnections("tcp4", TCP_BIG_ENDIAN, Map.of(), ByteOrder.BIG_ENDIAN).get(0);
        assertThat(conn.getLocalAddress(), is(ParseUtil.parseIpv4AddressToBytes("148.100.75.242")));
        assertThat(conn.getLocalPort(), is(22));
        assertThat(conn.getForeignAddress(), is(ParseUtil.parseIpv4AddressToBytes("73.83.45.212")));
        assertThat(conn.getForeignPort(), is(51011));
        assertThat(conn.getState(), is(TcpState.ESTABLISHED));
        assertThat("transmit queue", conn.getTransmitQueue(), is(0x48));

        IPConnection conn6 = LinuxInternetProtocolStats
                .parseConnections("tcp6", TCP6_BIG_ENDIAN, Map.of(), ByteOrder.BIG_ENDIAN).get(0);
        assertThat(conn6.getLocalAddress(), is(ParseUtil.parseIpv6AddressToBytes("2620:91:0:657:f816:3eff:fe5f:2052")));
        assertThat(conn6.getLocalPort(), is(50123));
        assertThat(conn6.getState(), is(TcpState.LISTEN));
    }
}
