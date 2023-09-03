/*
 * Copyright 2020-2023 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import oshi.SystemInfo;
import oshi.software.os.InternetProtocolStats.IPConnection;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

/**
 * Test IP stats
 */
@TestInstance(Lifecycle.PER_CLASS)
class InternetProtocolStatsTest {

    private InternetProtocolStats ipStats = null;

    @BeforeAll
    void setUp() {
        SystemInfo si = new SystemInfo();
        this.ipStats = si.getOperatingSystem().getInternetProtocolStats();
    }

    @Test
    void testTCPStats() {
        TcpStats tcp4 = ipStats.getTCPv4Stats();
        assertThat("IPV4 TCP connections should be 0 or higher", tcp4.getConnectionsEstablished(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP active connections should be 0 or higher", tcp4.getConnectionsActive(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP passive connections should be 0 or higher", tcp4.getConnectionsPassive(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP connection failures should be 0 or higher", tcp4.getConnectionFailures(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP connections reset should be 0 or higher", tcp4.getConnectionsReset(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP segments received should be 0 or higher", tcp4.getSegmentsReceived(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP segments sent should be 0 or higher", tcp4.getSegmentsSent(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP segments retransmitted should be 0 or higher", tcp4.getSegmentsRetransmitted(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP errors should be or higher ", tcp4.getInErrors(), is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 TCP segments transmitted with the reset flag should be 0 or higher", tcp4.getOutResets(),
                is(greaterThanOrEqualTo(0L)));

        TcpStats tcp6 = ipStats.getTCPv6Stats();
        assertThat("IPV6 TCP connections should be 0 or higher", tcp6.getConnectionsEstablished(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP active connections should be 0 or higher", tcp6.getConnectionsActive(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP passive connections should be 0 or higher", tcp6.getConnectionsPassive(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP connection failures should be 0 or higher", tcp6.getConnectionFailures(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP connections reset should be 0 or higher", tcp6.getConnectionsReset(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP segments received should be 0 or higher", tcp6.getSegmentsReceived(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP segments sent should be 0 or higher", tcp6.getSegmentsSent(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP segments retransmitted should be 0 or higher", tcp6.getSegmentsRetransmitted(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP errors should be or higher ", tcp6.getInErrors(), is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 TCP segments transmitted with the reset flag should be 0 or higher", tcp6.getOutResets(),
                is(greaterThanOrEqualTo(0L)));

        assertThat("IPV4 TCP stats shouldn't be null", tcp4.toString(), is(notNullValue()));
        assertThat("IPV6 TCP stats shouldn't be null", tcp6.toString(), is(notNullValue()));
    }

    @Test
    void testUDPStats() {
        UdpStats udp4 = ipStats.getUDPv4Stats();
        assertThat("IPV4 UDP datagrams delivered to UDP users should be 0 or higher", udp4.getDatagramsReceived(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 UDP datagrams sent should be 0 or higher", udp4.getDatagramsSent(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV4 UDP datagrams received with no application at the destination port should be 0 or higher",
                udp4.getDatagramsNoPort(), is(greaterThanOrEqualTo(0L)));
        assertThat(
                "IPV4 UDP datagrams received that could not be delivered for reasons other than lack of application at the destination port should be 0 or higher",
                udp4.getDatagramsReceivedErrors(), is(greaterThanOrEqualTo(0L)));

        UdpStats udp6 = ipStats.getUDPv6Stats();
        assertThat("IPV6 UDP datagrams delivered to UDP users should be 0 or higher", udp6.getDatagramsReceived(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 UDP datagrams sent should be 0 or higher", udp6.getDatagramsSent(),
                is(greaterThanOrEqualTo(0L)));
        assertThat("IPV6 UDP datagrams received with no application at the destination port should be 0 or higher",
                udp6.getDatagramsNoPort(), is(greaterThanOrEqualTo(0L)));
        assertThat(
                "IPV6 UDP datagrams received that could not be delivered for reasons other than lack of application at the destination port should be 0 or higher",
                udp6.getDatagramsReceivedErrors(), is(greaterThanOrEqualTo(0L)));

        assertThat("IPV4 UDP stats shouldn't be null", udp4.toString(), is(notNullValue()));
        assertThat("IPV6 UDP stats shouldn't be null", udp6.toString(), is(notNullValue()));
    }

    @Test
    void testGetConnections() {
        for (IPConnection conn : ipStats.getConnections()) {
            assertThat("Protocol name is not null or empty", conn.getType(),
                    allOf(is(not(emptyString())), is(notNullValue())));
            if (conn.getType().contains("4") || conn.getType().contains("46")) {
                assertThat("Local address array size should be 0, or 4", conn.getLocalAddress().length,
                        anyOf(is(0), is(4)));
                assertThat("Foreign address rray size should be 0, or 4", conn.getForeignAddress().length,
                        anyOf(is(0), is(4)));
            } else {
                assertThat("Local address array size should be 0, or 16", conn.getLocalAddress().length,
                        anyOf(is(0), is(16)));
                assertThat("Foreign address rray size should be 0, or 16", conn.getForeignAddress().length,
                        anyOf(is(0), is(16)));
            }
            assertThat("Local port must be a 16 bit value", conn.getLocalPort(),
                    allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(0xffff)));
            assertThat("Foreign port must be a 16 bit value", conn.getLocalPort(),
                    allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(0xffff)));
            assertThat("Transmit queue msut be nonnegative", conn.getTransmitQueue(), greaterThanOrEqualTo(0));
            assertThat("Receive queue msut be nonnegative", conn.getReceiveQueue(), greaterThanOrEqualTo(0));
            assertThat("Connection state must not be null", conn.getState(), is(notNullValue()));
            assertThat("Owning Process ID must be -1 or higher", conn.getowningProcessId(), greaterThanOrEqualTo(-1));
            assertThat("Connection toString must not be null", conn.toString(), is(notNullValue()));
        }
    }
}
