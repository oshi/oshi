/*
 * Copyright 2020-2025 The OSHI Project Contributors
 * SPDX-License-Identifier: MIT
 */
package oshi.software.os.linux;

import static oshi.software.os.InternetProtocolStats.TcpState.CLOSED;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSE_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.CLOSING;
import static oshi.software.os.InternetProtocolStats.TcpState.ESTABLISHED;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT_1;
import static oshi.software.os.InternetProtocolStats.TcpState.FIN_WAIT_2;
import static oshi.software.os.InternetProtocolStats.TcpState.LAST_ACK;
import static oshi.software.os.InternetProtocolStats.TcpState.LISTEN;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_RECV;
import static oshi.software.os.InternetProtocolStats.TcpState.SYN_SENT;
import static oshi.software.os.InternetProtocolStats.TcpState.TIME_WAIT;
import static oshi.software.os.InternetProtocolStats.TcpState.UNKNOWN;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.software.common.AbstractInternetProtocolStats;
import oshi.util.FileUtil;
import oshi.util.ParseUtil;
import oshi.util.platform.linux.ProcPath;
import oshi.util.tuples.Pair;

/**
 * Internet Protocol Stats implementation
 */
@ThreadSafe
public class LinuxInternetProtocolStats extends AbstractInternetProtocolStats {

    private final String TCP_COLON = "Tcp:";
    private final String UDP_COLON = "Udp:";
    private final String UDP6 = "Udp6";

    private enum TcpStat {
        TcpColon, RtoAlgorithm, RtoMin, RtoMax, MaxConn, ActiveOpens, PassiveOpens, AttemptFails, EstabResets,
        CurrEstab, InSegs, OutSegs, RetransSegs, InErrs, OutRsts, InCsumErrors;
    }

    private enum UdpStat {
        UdpColon, OutDatagrams, InDatagrams, NoPorts, InErrors, RcvbufErrors, SndbufErrors, InCsumErrors, IgnoredMulti,
        MemErrors;
    }

    @Override
    public TcpStats getTCPv4Stats() {
        byte[] fileBytes = FileUtil.readAllBytes(ProcPath.SNMP, true);
        List<String> lines = ParseUtil.parseByteArrayToStrings(fileBytes);
        Map<TcpStat, Long> tcpData = new EnumMap<>(TcpStat.class);

        for (int line = 0; line < lines.size() - 1; line += 2) {
            if (lines.get(line).startsWith(TCP_COLON) && lines.get(line + 1).startsWith(TCP_COLON)) {
                Map<TcpStat, String> parsedData = ParseUtil.stringToEnumMap(TcpStat.class, lines.get(line + 1), ' ');
                for (Map.Entry<TcpStat, String> entry : parsedData.entrySet()) {
                    if (entry.getKey().equals(TcpStat.TcpColon)) {
                        continue; // Skip "Tcp:"
                    }
                    tcpData.put(entry.getKey(), Long.parseLong(entry.getValue()));
                }
                break;
            }
        }

        return new TcpStats(tcpData.getOrDefault(TcpStat.CurrEstab, 0L), tcpData.getOrDefault(TcpStat.ActiveOpens, 0L),
                tcpData.getOrDefault(TcpStat.PassiveOpens, 0L), tcpData.getOrDefault(TcpStat.AttemptFails, 0L),
                tcpData.getOrDefault(TcpStat.EstabResets, 0L), tcpData.getOrDefault(TcpStat.OutSegs, 0L),
                tcpData.getOrDefault(TcpStat.InSegs, 0L), tcpData.getOrDefault(TcpStat.RetransSegs, 0L),
                tcpData.getOrDefault(TcpStat.InErrs, 0L), tcpData.getOrDefault(TcpStat.OutRsts, 0L));
    }

    @Override
    public UdpStats getUDPv4Stats() {
        byte[] fileBytes = FileUtil.readAllBytes(ProcPath.SNMP, true);
        List<String> lines = ParseUtil.parseByteArrayToStrings(fileBytes);
        Map<UdpStat, Long> udpData = new EnumMap<>(UdpStat.class);

        for (int line = 0; line < lines.size() - 1; line += 2) {
            if (lines.get(line).startsWith(UDP_COLON) && lines.get(line + 1).startsWith(UDP_COLON)) {
                Map<UdpStat, String> parsedData = ParseUtil.stringToEnumMap(UdpStat.class, lines.get(line + 1), ' ');
                for (Map.Entry<UdpStat, String> entry : parsedData.entrySet()) {
                    if (entry.getKey().equals(UdpStat.UdpColon)) {
                        continue; // Skip "Udp:"
                    }
                    udpData.put(entry.getKey(), Long.parseLong(entry.getValue()));
                }
                break;
            }
        }

        return new UdpStats(udpData.getOrDefault(UdpStat.OutDatagrams, 0L),
                udpData.getOrDefault(UdpStat.InDatagrams, 0L), udpData.getOrDefault(UdpStat.NoPorts, 0L),
                udpData.getOrDefault(UdpStat.InErrors, 0L));
    }

    @Override
    public UdpStats getUDPv6Stats() {
        byte[] fileBytes = FileUtil.readAllBytes(ProcPath.SNMP6, true);
        List<String> lines = ParseUtil.parseByteArrayToStrings(fileBytes);
        long inDatagrams = 0, noPorts = 0, inErrors = 0, outDatagrams = 0;
        int foundUDPv6StatsCount = 0;

        // Traverse bottom-to-top for efficiency as the /etc/proc/snmp6 file follows sequential format -> ip6, icmp6,
        // udp6 stats
        for (int line = lines.size() - 1; line >= 0 && foundUDPv6StatsCount < 4; line--) {
            if (lines.get(line).startsWith(UDP6)) {
                String[] parts = lines.get(line).split("\\s+");
                switch (parts[0]) {
                case "Udp6InDatagrams":
                    inDatagrams = Long.parseLong(parts[1]);
                    foundUDPv6StatsCount++;
                    break;
                case "Udp6NoPorts":
                    noPorts = Long.parseLong(parts[1]);
                    foundUDPv6StatsCount++;
                    break;
                case "Udp6InErrors":
                    inErrors = Long.parseLong(parts[1]);
                    foundUDPv6StatsCount++;
                    break;
                case "Udp6OutDatagrams":
                    outDatagrams = Long.parseLong(parts[1]);
                    foundUDPv6StatsCount++;
                    break;
                default:
                    break;
                }
            }
        }

        return new UdpStats(inDatagrams, noPorts, inErrors, outDatagrams);
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
                String[] split = ParseUtil.whitespaces.split(s.trim());
                if (split.length > 9) {
                    Pair<byte[], Integer> lAddr = parseIpAddr(split[1]);
                    Pair<byte[], Integer> fAddr = parseIpAddr(split[2]);
                    TcpState state = stateLookup(ParseUtil.hexStringToInt(split[3], 0));
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

    private static TcpState stateLookup(int state) {
        switch (state) {
        case 0x01:
            return ESTABLISHED;
        case 0x02:
            return SYN_SENT;
        case 0x03:
            return SYN_RECV;
        case 0x04:
            return FIN_WAIT_1;
        case 0x05:
            return FIN_WAIT_2;
        case 0x06:
            return TIME_WAIT;
        case 0x07:
            return CLOSED;
        case 0x08:
            return CLOSE_WAIT;
        case 0x09:
            return LAST_ACK;
        case 0x0A:
            return LISTEN;
        case 0x0B:
            return CLOSING;
        case 0x00:
        default:
            return UNKNOWN;
        }
    }
}
