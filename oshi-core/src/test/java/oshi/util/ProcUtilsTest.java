/*
 * Copyright 2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@EnabledOnOs(OS.LINUX)
public class ProcUtilsTest {

    @Test
    void testRawNetNetstat() {
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-netstat.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.parseNestedStatistics(resource);

        assertThat(results.keySet(), containsInAnyOrder("TcpExt", "IpExt", "MPTcpExt", "BadExt", "MoreBadExt"));
        assertThat(results.get("TcpExt").get("SyncookiesSent"), is(6L));
        assertThat(results.get("TcpExt").get("TCPAODroppedIcmps"), is(3L));
        assertThat(results.get("IpExt").get("InNoRoutes"), is(55L));
        assertThat(results.get("MPTcpExt").get("MPCurrEstab"), is(1L));
        assertThat(results.get("BadExt").get("One"), is(1L));
        assertThat(results.get("MoreBadExt").get("Six"), is(6L));
    }

    @Test
    void testRawNetNetstatWithLimitedKeys() {
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-netstat.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.parseNestedStatistics(resource, "IpExt");

        assertThat(results.keySet(), contains("IpExt"));
        assertThat(results.get("IpExt").get("InNoRoutes"), is(55L));
    }

    @Test
    void testRawNetSnmp() {
        String resource = ProcUtilsTest.class.getResource("sample-proc-net-snmp.txt").getFile();

        Map<String, Map<String, Long>> results = ProcUtil.parseNestedStatistics(resource);

        assertThat(results.keySet(), containsInAnyOrder("Ip", "Icmp", "IcmpMsg", "Tcp", "Udp", "UdpLite"));
        assertThat(results.get("Tcp").get("ActiveOpens"), is(1892L));
        assertThat(results.get("Ip").get("OutTransmits"), is(66296L));
        assertThat(results.get("Icmp").get("InMsgs"), is(184L));
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

}
