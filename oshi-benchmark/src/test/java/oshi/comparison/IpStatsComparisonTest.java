/*
 * Copyright 2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.comparison;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.condition.DisabledIf;

import oshi.SystemInfo;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.InternetProtocolStats.TcpStats;
import oshi.software.os.InternetProtocolStats.UdpStats;
import oshi.util.ExecutingCommand;
import oshi.util.ParseUtil;
import oshi.util.PlatformEnum;

/**
 * Compares the TCP and UDP counters read natively from the {@code net.inet.tcp.stats} and {@code net.inet.udp.stats}
 * sysctls against the same counters printed by {@code netstat -s}.
 * <p>
 * The native path reads selected counters at fixed offsets in a kernel structure that has no version field, and those
 * offsets have moved between releases: FreeBSD 14.0 inserted a counter mid-structure. A wrong offset still yields a
 * plausible number, so only a second reading of the same counter can catch it. {@code netstat} prints each counter with
 * its own label, so this test parses that output itself rather than through the production parser, whose mapping serves
 * several platforms' wording.
 * <p>
 * The counters only grow, so each is checked to lie between a native read taken before running {@code netstat} and one
 * taken after.
 */
@DisabledIf("isNotNativeSysctl")
class IpStatsComparisonTest {

    private static final Pattern COUNT_LINE = Pattern.compile("^\\s*(\\d+) (.+)$");
    private static final Pattern DROPS = Pattern.compile("\\(including (\\d+) drops?\\)");

    /** Only the platforms reading these counters from a native structure. */
    static boolean isNotNativeSysctl() {
        PlatformEnum p = PlatformEnum.getCurrentPlatform();
        return p != PlatformEnum.MACOS && p != PlatformEnum.FREEBSD;
    }

    /**
     * Parses {@code netstat -s -p <protocol>} into counts keyed by label, with the count and any parenthesized byte
     * count removed and plurals reduced to the singular, so {@code "1 data packet (36 bytes)"} and
     * {@code "2 data packets (72 bytes)"} share the key {@code "data packet"}. A count inside the label, as in
     * {@code "connections closed (including 3 drops)"}, is kept under its own key, here {@code "drop"}.
     */
    private static Map<String, Long> netstat(String protocol) {
        Map<String, Long> counts = new HashMap<>();
        for (String line : ExecutingCommand.runNative("netstat -s -p " + protocol)) {
            Matcher m = COUNT_LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String label = m.group(2);
            Matcher drops = DROPS.matcher(label);
            if (drops.find()) {
                counts.put("drop", ParseUtil.parseLongOrDefault(drops.group(1), 0L));
            }
            String key = label.replaceAll("\\s*\\([^)]*\\)", "").replaceAll("(\\w)s\\b", "$1").trim();
            counts.putIfAbsent(key, ParseUtil.parseLongOrDefault(m.group(1), 0L));
        }
        return counts;
    }

    private static InternetProtocolStats freshStats() {
        // A new SystemInfo for each read, so neither read is served from the other's memoized value
        return new SystemInfo().getOperatingSystem().getInternetProtocolStats();
    }

    /** A count netstat printed, or -1 if it printed no such line, which no counter range contains. */
    private static long count(Map<String, Long> netstat, String... keys) {
        long sum = 0;
        for (String key : keys) {
            long value = netstat.getOrDefault(key, -1L);
            if (value < 0) {
                return -1L;
            }
            sum += value;
        }
        return sum;
    }

    /** Checks that a counter read by netstat lies between the native reads taken before and after it. */
    private static <T> void between(List<String> failures, String name, T before, T after, ToLongFunction<T> field,
            long command) {
        long low = field.applyAsLong(before);
        long high = field.applyAsLong(after);
        if (command < low || command > high) {
            failures.add(name + ": netstat " + command + " not within native [" + low + ", " + high + "]");
        }
    }

    @RetryingComparison
    void testTcpAgreesWithNetstat() {
        TcpStats before = freshStats().getTCPv4Stats();
        Map<String, Long> cmd = netstat("tcp");
        TcpStats after = freshStats().getTCPv4Stats();
        assertThat(cmd).as("netstat -s -p tcp counters").containsKeys("connection request", "connection accept",
                "embryonic connection dropped", "drop", "data packet retransmitted");

        List<String> failures = new ArrayList<>();
        between(failures, "active opens", before, after, TcpStats::getConnectionsActive,
                count(cmd, "connection request"));
        between(failures, "passive opens", before, after, TcpStats::getConnectionsPassive,
                count(cmd, "connection accept"));
        between(failures, "failures", before, after, TcpStats::getConnectionFailures,
                count(cmd, "embryonic connection dropped"));
        between(failures, "resets", before, after, TcpStats::getConnectionsReset, count(cmd, "drop"));
        between(failures, "retransmitted", before, after, TcpStats::getSegmentsRetransmitted,
                count(cmd, "data packet retransmitted"));
        // Without root, macOS estimates these three from IP-level counters instead of reading them
        if (PlatformEnum.getCurrentPlatform() == PlatformEnum.FREEBSD) {
            between(failures, "sent", before, after, TcpStats::getSegmentsSent, count(cmd, "data packet"));
            between(failures, "received", before, after, TcpStats::getSegmentsReceived,
                    count(cmd, "packet received in-sequence"));
            between(failures, "in errors", before, after, TcpStats::getInErrors,
                    count(cmd, "discarded for bad checksum", "discarded for bad header offset field",
                            "discarded because packet too short"));
        }
        assertThat(failures).as("TCP counters disagreeing with netstat").isEmpty();
    }

    @RetryingComparison
    void testUdpAgreesWithNetstat() {
        UdpStats before = freshStats().getUDPv4Stats();
        Map<String, Long> cmd = netstat("udp");
        UdpStats after = freshStats().getUDPv4Stats();
        assertThat(cmd).as("netstat -s -p udp counters").containsKeys("datagram received", "datagram output",
                "with incomplete header", "with bad data length field", "with bad checksum", "dropped due to no socket",
                "broadcast/multicast datagram undelivered");

        List<String> failures = new ArrayList<>();
        between(failures, "received", before, after, UdpStats::getDatagramsReceived, count(cmd, "datagram received"));
        between(failures, "sent", before, after, UdpStats::getDatagramsSent, count(cmd, "datagram output"));
        between(failures, "receive errors", before, after, UdpStats::getDatagramsReceivedErrors,
                count(cmd, "with incomplete header", "with bad data length field", "with bad checksum"));
        // netstat's "undelivered" line is udps_noportbcast + udps_noportmcast. macOS reports only udps_noportmcast
        if (PlatformEnum.getCurrentPlatform() == PlatformEnum.FREEBSD) {
            between(failures, "no port", before, after, UdpStats::getDatagramsNoPort,
                    count(cmd, "dropped due to no socket", "broadcast/multicast datagram undelivered"));
        }
        assertThat(failures).as("UDP counters disagreeing with netstat").isEmpty();
    }
}
