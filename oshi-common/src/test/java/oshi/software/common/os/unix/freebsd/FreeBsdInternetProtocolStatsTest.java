/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.freebsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.jupiter.api.Test;

import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

class FreeBsdInternetProtocolStatsTest {

    /**
     * Builds a structure of {@code uint64_t} counters in which each counter holds its own index plus 1000, so a value
     * read from the wrong offset is recognizable.
     */
    private static byte[] counters(int size) {
        ByteBuffer bb = ByteBuffer.allocate(size).order(ByteOrder.nativeOrder());
        for (int i = 0; i < size / Long.BYTES; i++) {
            bb.putLong(i * Long.BYTES, 1000L + i);
        }
        return bb.array();
    }

    // Sizes: 10.x and 11.x; 12.x and 13.x; 14.x; 15.0; 16-CURRENT
    @Test
    void testParseTcpstat() {
        assertTcpstat(880, 0);
        assertTcpstat(904, 0);
        assertTcpstat(984, 1);
        assertTcpstat(1000, 1);
        assertTcpstat(1048, 1);
    }

    // Counter indexes from sys/netinet/tcp_var.h; shift is 1 from FreeBSD 14.0's tcps_progdrops onward
    private static void assertTcpstat(int size, int shift) {
        TcpStats stats = FreeBsdInternetProtocolStats.parseTcpstat(counters(size), 42L);
        assertNotNull(stats);
        assertThat(stats.getConnectionsEstablished(), is(42L));
        assertThat(stats.getConnectionsActive(), is(1000L)); // tcps_connattempt
        assertThat(stats.getConnectionsPassive(), is(1001L)); // tcps_accepts
        assertThat(stats.getConnectionsReset(), is(1003L)); // tcps_drops
        assertThat(stats.getConnectionFailures(), is(1004L)); // tcps_conndrops
        assertThat(stats.getSegmentsSent(), is(1017L + shift)); // tcps_sndpack
        assertThat(stats.getSegmentsRetransmitted(), is(1019L + shift)); // tcps_sndrexmitpack
        assertThat(stats.getSegmentsReceived(), is(1028L + shift)); // tcps_rcvpack
        // tcps_rcvbadsum + tcps_rcvbadoff + tcps_rcvshort
        assertThat(stats.getInErrors(), is(1030L + 1031L + 1033L + 3L * shift));
        assertThat(stats.getOutResets(), is(0L));
    }

    // FreeBSD 9 and earlier (u_long fields), or a size between the known layouts
    @Test
    void testParseTcpstatUnknownLayout() {
        for (int size : new int[] { 0, 440, 872, 912, 976 }) {
            assertNull(FreeBsdInternetProtocolStats.parseTcpstat(counters(size), 0L), "size " + size);
        }
    }

    @Test
    void testParseUdpstat() {
        UdpStats stats = FreeBsdInternetProtocolStats.parseUdpstat(counters(112));
        assertNotNull(stats);
        assertThat(stats.getDatagramsReceived(), is(1000L)); // udps_ipackets
        assertThat(stats.getDatagramsSent(), is(1010L)); // udps_opackets
        // udps_noport + udps_noportbcast + udps_noportmcast
        assertThat(stats.getDatagramsNoPort(), is(1005L + 1006L + 1012L));
        // udps_hdrops + udps_badsum + udps_badlen
        assertThat(stats.getDatagramsReceivedErrors(), is(1001L + 1002L + 1004L));
    }

    @Test
    void testParseUdpstatTooShort() {
        assertNull(FreeBsdInternetProtocolStats.parseUdpstat(counters(56)));
        assertNull(FreeBsdInternetProtocolStats.parseUdpstat(new byte[0]));
    }
}
