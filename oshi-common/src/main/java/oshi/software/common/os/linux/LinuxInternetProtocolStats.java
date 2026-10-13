/*
 * Copyright 2020-2026 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.common.os.linux;

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
        List<IPConnection> conns = new ArrayList<>();
        for (String s : FileUtil.readFile(ProcPath.NET + "/" + protocol + (ipver == 6 ? "6" : ""))) {
            if (s.indexOf(':') >= 0) {
                String[] split = ParseUtil.whitespaces.split(s.trim(), -1);
                if (split.length > 9) {
                    Pair<byte[], Integer> lAddr = parseIpAddr(split[1]);
                    Pair<byte[], Integer> fAddr = parseIpAddr(split[2]);
                    TcpState state = TcpState.fromLinuxState(ParseUtil.hexStringToInt(split[3], 0));
                    Pair<Integer, Integer> txQrxQ = parseHexColonHex(split[4]);
                    long inode = ParseUtil.parseLongOrDefault(split[9], 0);
                    conns.add(new IPConnection(protocol + ipver, lAddr.getA(), lAddr.getB(), fAddr.getA(), fAddr.getB(),
                            state, txQrxQ.getA(), txQrxQ.getB(), pidMap.getOrDefault(inode, -1)));
                }
            }
        }
        return conns;
    }

    private static Pair<byte[], Integer> parseIpAddr(String s) {
        int colon = s.indexOf(':');
        if (colon > 0 && colon < s.length()) {
            byte[] first = ParseUtil.hexStringToByteArray(s.substring(0, colon));
            // Bytes are in __be32 endianness. we must invert each set of 4 bytes
            for (int i = 0; i + 3 < first.length; i += 4) {
                byte tmp = first[i];
                first[i] = first[i + 3];
                first[i + 3] = tmp;
                tmp = first[i + 1];
                first[i + 1] = first[i + 2];
                first[i + 2] = tmp;
            }
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
