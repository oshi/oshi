/*
 * Copyright 2020-2024 The OSHI Project Contributors
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import oshi.annotation.concurrent.ThreadSafe;
import oshi.driver.linux.proc.ProcessStat;
import oshi.driver.unix.NetStat;
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
        CurrEstab, ActiveOpens, PassiveOpens, AttemptFails, EstabResets, OutSegs, InSegs, RetransSegs, InErrs, OutRsts
    }

    private enum UdpStat {
        OutDatagrams, InDatagrams, NoPorts, InErrors
    }

    @Override
    public TcpStats getTCPv4Stats() {
        byte[] fileBytes = FileUtil.readAllBytes(ProcPath.SNMP, true);
        String content = new String(fileBytes);
        List<String> lines = Arrays.asList(content.split("\\n"));
        Map<TcpStat, Long> tcpData = new EnumMap<>(TcpStat.class);

        for (int line = 0; line < lines.size() - 1; line++) {
            if (lines.get(line).startsWith(TCP_COLON) && lines.get(line + 1).startsWith(TCP_COLON)) {
                String[] headers = lines.get(line).split("\\s+");
                String[] values = lines.get(line + 1).split("\\s+");
                for (int header = 1; header < headers.length; header++) {
                    try {
                        TcpStat key = TcpStat.valueOf(headers[header]);
                        long value = Long.parseLong(values[header]);
                        tcpData.put(key, value);
                    } catch (IllegalArgumentException e) {
                        // Ignore fields that are not in TcpStat enum
                    }
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
        String content = new String(fileBytes);
        List<String> lines = Arrays.asList(content.split("\\n"));
        Map<UdpStat, Long> udpData = new EnumMap<>(UdpStat.class);

        for (int line = 0; line < lines.size() - 1; line++) {
            if (lines.get(line).startsWith(UDP_COLON) && lines.get(line + 1).startsWith(UDP_COLON)) {
                String[] headers = lines.get(line).split("\\s+");
                String[] values = lines.get(line + 1).split("\\s+");
                for (int header = 1; header < headers.length; header++) {
                    try {
                        UdpStat key = UdpStat.valueOf(headers[header]);
                        long value = Long.parseLong(values[header]);
                        udpData.put(key, value);
                    } catch (IllegalArgumentException e) {
                        // Ignore fields that are not in UdpStat enum
                    }
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
        String content = new String(fileBytes);
        List<String> lines = Arrays.asList(content.split("\\n"));
        Map<UdpStat, Long> udpData = new EnumMap<>(UdpStat.class);

        for (String line : lines) {
            String[] parts = line.split("\\t");
            if (parts.length == 2 && parts[0].startsWith(UDP6)) {
                try {
                    // Remove "Udp6" prefix and map to enum
                    String keyName = parts[0].substring(4);
                    UdpStat key = UdpStat.valueOf(keyName);
                    long value = Long.parseLong(parts[1]);
                    udpData.put(key, value);
                } catch (IllegalArgumentException e) {
                    // Ignore fields that are not in UdpStat enum
                }
            }
        }

        return new UdpStats(udpData.getOrDefault(UdpStat.OutDatagrams, 0L),
            udpData.getOrDefault(UdpStat.InDatagrams, 0L), udpData.getOrDefault(UdpStat.NoPorts, 0L),
            udpData.getOrDefault(UdpStat.InErrors, 0L));
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
