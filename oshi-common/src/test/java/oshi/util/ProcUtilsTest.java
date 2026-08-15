/*
 * Copyright 2025-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
class ProcUtilsTest {

    @Test
    void testRawNetNetstat() {
        // The fixture contains an empty line, which splits to a single empty element and is left with nothing after
        // the leading-whitespace shift; parsing must skip it rather than index into an empty array
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-netstat.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.parseNestedStatistics(resource);

        assertThat(results.keySet(), containsInAnyOrder("TcpExt", "IpExt", "MPTcpExt", "BadExt", "MoreBadExt"));
        assertThat(stat(results, "TcpExt", "SyncookiesSent"), is(6L));
        assertThat(stat(results, "TcpExt", "TCPAODroppedIcmps"), is(3L));
        assertThat(stat(results, "IpExt", "InNoRoutes"), is(55L));
        assertThat(stat(results, "MPTcpExt", "MPCurrEstab"), is(1L));
        assertThat(stat(results, "BadExt", "One"), is(1L));
        assertThat(stat(results, "MoreBadExt", "Six"), is(6L));
    }

    /**
     * Fetches a nested statistic, failing the test with the missing key rather than throwing on a null dereference.
     */
    private static long stat(Map<String, Map<String, Long>> results, String group, String name) {
        Map<String, Long> stats = results.get(group);
        assertNotNull(stats, () -> "no such group: " + group);
        Long value = stats.get(name);
        assertNotNull(value, () -> "no such statistic: " + group + "." + name);
        return value;
    }

    @Test
    void testRawNetNetstatWithLimitedKeys() {
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-netstat.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.parseNestedStatistics(resource, "IpExt");

        assertThat(results.keySet(), contains("IpExt"));
        assertThat(stat(results, "IpExt", "InNoRoutes"), is(55L));
    }

    @Test
    void testRawNetSnmp() {
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-snmp.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.parseNestedStatistics(resource);

        assertThat(results.keySet(), containsInAnyOrder("Ip", "Icmp", "IcmpMsg", "Tcp", "Udp", "UdpLite"));
        assertThat(stat(results, "Tcp", "ActiveOpens"), is(1892L));
        assertThat(stat(results, "Ip", "OutTransmits"), is(66296L));
        assertThat(stat(results, "Icmp", "InMsgs"), is(184L));
    }

    @Test
    void testRawNetSnmp6() {
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-snmp6.txt").getFile();

        Map<String, Long> results = ProcUtil.parseStatistics(resource);

        assertThat(results.keySet(), hasSize(91));
        assertThat(results.get("Ip6InReceives"), is(8026L));
        assertThat(results.get("Ip6OutMcastOctets"), is(45957L));
        assertThat(results.get("UdpLite6MemErrors"), is(1L));
        assertThat(results.get("IndentedEntry"), is(37L));
    }

    @Test
    void testParseStatisticsWithTrailingWhitespace() {
        // A trailing space on the line must not prevent it from producing exactly two fields
        Map<String, Long> results = ProcUtil
                .parseStatistics(Collections.singletonList("SomeStatistic             12345 "), ParseUtil.whitespaces);

        assertThat(results.get("SomeStatistic"), is(12345L));
    }

}
