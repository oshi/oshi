/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.ProcUtil;
import oshi.util.driver.linux.proc.ProcessStat;
import oshi.util.linux.ProcPath;
import oshi.util.tuples.Pair;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class LinuxInternetProtocolStats extends AbstractInternetProtocolStats {

    /**
     * Default constructor.
     */
    protected LinuxInternetProtocolStats() {
    }

    private static final String TCP = "Tcp";
    private static final String UDP = "Udp";

    @Override
    public TcpStats getTCPv4Stats() {
        return parseTcp4Stats(snmpStats(TCP));
    }

    /**
     * Builds TCPv4 statistics from the {@code Tcp} counters of {@code /proc/net/snmp}.
     *
     * @param tcp a map of counter names to values
     * @return the statistics, with zeros for any counter not present
     */
    static TcpStats parseTcp4Stats(Map<String, Long> tcp) {
        return new TcpStats(tcp.getOrDefault("CurrEstab", 0L), tcp.getOrDefault("ActiveOpens", 0L),
                tcp.getOrDefault("PassiveOpens", 0L), tcp.getOrDefault("AttemptFails", 0L),
                tcp.getOrDefault("EstabResets", 0L), tcp.getOrDefault("OutSegs", 0L), tcp.getOrDefault("InSegs", 0L),
                tcp.getOrDefault("RetransSegs", 0L), tcp.getOrDefault("InErrs", 0L), tcp.getOrDefault("OutRsts", 0L));
    }

    @Override
    public UdpStats getUDPv4Stats() {
        return parseUdp4Stats(snmpStats(UDP));
    }

    /**
     * Builds UDPv4 statistics from the {@code Udp} counters of {@code /proc/net/snmp}.
     *
     * @param udp a map of counter names to values
     * @return the statistics, with zeros for any counter not present
     */
    static UdpStats parseUdp4Stats(Map<String, Long> udp) {
        return new UdpStats(udp.getOrDefault("OutDatagrams", 0L), udp.getOrDefault("InDatagrams", 0L),
                udp.getOrDefault("NoPorts", 0L), udp.getOrDefault("InErrors", 0L));
    }

    @Override
    public UdpStats getUDPv6Stats() {
        return parseUdp6Stats(ProcUtil.parseStatistics(ProcPath.SNMP6));
    }

    /**
     * Builds UDPv6 statistics from the counters of {@code /proc/net/snmp6}.
     *
     * @param snmp6 a map of counter names to values, as read from the whole file
     * @return the statistics, with zeros for any counter not present
     */
    static UdpStats parseUdp6Stats(Map<String, Long> snmp6) {
        return new UdpStats(snmp6.getOrDefault("Udp6OutDatagrams", 0L), snmp6.getOrDefault("Udp6InDatagrams", 0L),
                snmp6.getOrDefault("Udp6NoPorts", 0L), snmp6.getOrDefault("Udp6InErrors", 0L));
    }

    private static Map<String, Long> snmpStats(String key) {
        return ProcUtil.parseNestedStatistics(ProcPath.SNMP, key).getOrDefault(key, Collections.emptyMap());
    }

    @Override
    public List<IPConnection> getConnections() {
        List<IPConnection> conns = new ArrayList<>();
        Map<Long, Integer> pidMap = ProcessStat.querySocketToPidMap();
        conns.addAll(queryConnections("tcp", 4, pidMap));
        conns.addAll(queryConnections("tcp", 6, pidMap));
        conns.addAll(queryConnections("udp", 4, pidMap));
        conns.addAll(queryConnections("udp", 6, pidMap));
        return conns;
    }

    private static List<IPConnection> queryConnections(String protocol, int ipver, Map<Long, Integer> pidMap) {
        return parseConnections(protocol + ipver,
                FileUtil.readFile(ProcPath.NET + "/" + protocol + (ipver == 6 ? "6" : "")), pidMap,
                ByteOrder.nativeOrder());
    }

    /**
     * Parses the lines of one of {@code /proc/net/tcp}, {@code tcp6}, {@code udp} or {@code udp6}.
     *
     * @param type   the connection type to report, such as {@code tcp4}
     * @param lines  the lines of the file
     * @param pidMap a map of socket inode to owning process ID
     * @param order  the byte order of the host that wrote the lines
     * @return the parsed connections
     */
    static List<IPConnection> parseConnections(String type, List<String> lines, Map<Long, Integer> pidMap,
            ByteOrder order) {
        List<IPConnection> conns = new ArrayList<>();
        for (String s : lines) {
            if (s.indexOf(':') >= 0) {
                String[] split = ParseUtil.whitespaces.split(s.trim(), -1);
                if (split.length > 9) {
                    Pair<byte[], Integer> lAddr = parseIpAddr(split[1], order);
                    Pair<byte[], Integer> fAddr = parseIpAddr(split[2], order);
                    TcpState state = TcpState.fromLinuxState(ParseUtil.hexStringToInt(split[3], 0));
                    Pair<Integer, Integer> txQrxQ = parseHexColonHex(split[4]);
                    long inode = ParseUtil.parseLongOrDefault(split[9], 0);
                    conns.add(new IPConnection(type, lAddr.getA(), lAddr.getB(), fAddr.getA(), fAddr.getB(), state,
                            txQrxQ.getA(), txQrxQ.getB(), pidMap.getOrDefault(inode, -1)));
                }
            }
        }
        return conns;
    }

    private static Pair<byte[], Integer> parseIpAddr(String s, ByteOrder order) {
        int colon = s.indexOf(':');
        if (colon > 0 && colon < s.length()) {
            // Each 32-bit word of the __be32 address is printed with %08X, so in the host's byte order
            byte[] first = ParseUtil.hexWordsToByteArray(s.substring(0, colon), order);
            int second = ParseUtil.hexStringToInt(s.substring(colon + 1), 0);
            return new Pair<>(first, second);
        }
        return new Pair<>(new byte[0], 0);
    }

    private static Pair<Integer, Integer> parseHexColonHex(String s) {
        int colon = s.indexOf(':');
        if (colon > 0 && colon < s.length()) {
            int first = ParseUtil.hexStringToInt(s.substring(0, colon), 0);
            int second = ParseUtil.hexStringToInt(s.substring(colon + 1), 0);
            return new Pair<>(first, second);
        }
        return new Pair<>(0, 0);
    }
}
