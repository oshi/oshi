/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.unix.solaris;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;

class SolarisInternetProtocolStatsTest {

    @Test
    void testParseTcpStats() {
        // netstat -s -P tcp output: two stats per line separated by spaces
        List<String> netstat = Arrays.asList(//
                "tcpCurrEstab =    42   tcpActiveOpens =  1000", //
                "tcpPassiveOpens = 500   tcpAttemptFails =   10", //
                "tcpEstabResets =    5   tcpOutSegs    = 50000", //
                "tcpInSegs     = 60000   tcpRetransSegs =   200", //
                "tcpInErr      =     3   tcpOutRsts    =    15");
        TcpStats stats = SolarisInternetProtocolStats.parseTcpStats(netstat);
        assertThat(stats.getConnectionsEstablished(), is(42L));
        assertThat(stats.getConnectionsActive(), is(1000L));
        assertThat(stats.getConnectionsPassive(), is(500L));
        assertThat(stats.getConnectionFailures(), is(10L));
        assertThat(stats.getConnectionsReset(), is(5L));
        assertThat(stats.getSegmentsSent(), is(50000L));
        assertThat(stats.getSegmentsReceived(), is(60000L));
        assertThat(stats.getSegmentsRetransmitted(), is(200L));
        assertThat(stats.getInErrors(), is(3L));
        assertThat(stats.getOutResets(), is(15L));
    }

    @Test
    void testParseTcpStatsMixedPrefix() {
        // Real netstat output: tcpInErr is paired with a non-tcp stat (from IP section),
        // so splitOnPrefix finds only one "tcp" occurrence and the remainder contains "="
        List<String> netstat = Arrays.asList(//
                "tcpCurrEstab =    10   tcpActiveOpens =   100", //
                "tcpInErr     =     7   udpNoPorts    =    99");
        TcpStats stats = SolarisInternetProtocolStats.parseTcpStats(netstat);
        assertThat(stats.getConnectionsEstablished(), is(10L));
        assertThat(stats.getConnectionsActive(), is(100L));
        // tcpInErr uses getFirstIntValue so it extracts 7 even with trailing "udpNoPorts = 99"
        assertThat(stats.getInErrors(), is(7L));
    }

    @Test
    void testParseTcpStatsEmpty() {
        TcpStats stats = SolarisInternetProtocolStats.parseTcpStats(Collections.emptyList());
        assertThat(stats.getConnectionsEstablished(), is(0L));
        assertThat(stats.getSegmentsSent(), is(0L));
    }

    @Test
    void testParseUdpStats() {
        List<String> netstat = Arrays.asList(//
                "udpOutDatagrams = 10000   udpInDatagrams = 20000", //
                "udpNoPorts      =   100   udpInErrors    =    50");
        UdpStats stats = SolarisInternetProtocolStats.parseUdpStats(netstat);
        assertThat(stats.getDatagramsSent(), is(10000L));
        assertThat(stats.getDatagramsReceived(), is(20000L));
        assertThat(stats.getDatagramsNoPort(), is(100L));
        assertThat(stats.getDatagramsReceivedErrors(), is(50L));
    }

    @Test
    void testParseUdpStatsEmpty() {
        UdpStats stats = SolarisInternetProtocolStats.parseUdpStats(Collections.emptyList());
        assertThat(stats.getDatagramsSent(), is(0L));
        assertThat(stats.getDatagramsReceived(), is(0L));
    }

    @Test
    void testSplitOnPrefix() {
        String[] result = SolarisInternetProtocolStats.splitOnPrefix("tcpFoo = 1   tcpBar = 2", "tcp");
        assertThat(result[0], is("tcpFoo = 1"));
        assertThat(result[1], is("tcpBar = 2"));
    }

    @Test
    void testSplitOnPrefixSingleStat() {
        String[] result = SolarisInternetProtocolStats.splitOnPrefix("tcpFoo = 1", "tcp");
        assertThat(result[0], is("tcpFoo = 1"));
        assertThat(result[1], is(nullValue()));
    }

    @Test
    void testSplitOnPrefixNoMatch() {
        String[] result = SolarisInternetProtocolStats.splitOnPrefix("some random line", "tcp");
        assertThat(result[0], is(nullValue()));
        assertThat(result[1], is(nullValue()));
    }
}
